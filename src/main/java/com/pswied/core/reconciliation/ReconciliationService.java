package com.pswied.core.reconciliation;

import com.pswied.core.payment.PaymentRecord;
import com.pswied.core.payment.PaymentRecordRepository;
import com.pswied.core.payment.PaymentSource;
import com.pswied.core.payment.PaymentStatus;
import com.pswied.core.audit.AuditLog;
import com.pswied.core.audit.AuditLogRepository;
import com.pswied.core.exception.ExceptionSeverity;
import com.pswied.core.exception.PaymentException;
import com.pswied.core.exception.PaymentExceptionCategory;
import com.pswied.core.exception.PaymentExceptionRepository;
import com.pswied.core.exception.PaymentExceptionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReconciliationService {

    private final PaymentRecordRepository paymentRecordRepository;
    private final ReconciliationBatchRepository batchRepository;
    private final ReconciliationResultRepository resultRepository;
    private final PaymentExceptionRepository exceptionRepository;
    private final AuditLogRepository auditLogRepository;

    public ReconciliationService(
            PaymentRecordRepository paymentRecordRepository,
            ReconciliationBatchRepository batchRepository,
            ReconciliationResultRepository resultRepository,
            PaymentExceptionRepository exceptionRepository,
            AuditLogRepository auditLogRepository) {
        this.paymentRecordRepository = paymentRecordRepository;
        this.batchRepository = batchRepository;
        this.resultRepository = resultRepository;
        this.exceptionRepository = exceptionRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public ReconciliationBatchResponse run(String initiatedBy) {
        Instant startedAt = Instant.now();
        UUID batchId = UUID.randomUUID();
        ReconciliationBatch batch = new ReconciliationBatch(
                batchId, ReconciliationBatchStatus.RUNNING, startedAt, null, initiatedBy);
        batchRepository.save(batch);

        List<PaymentRecord> internalRecords =
                paymentRecordRepository.findBySourceOrderByValueDateAscTransactionReferenceAscIdAsc(
                        PaymentSource.INTERNAL);
        List<PaymentRecord> externalRecords =
                paymentRecordRepository.findBySourceOrderByValueDateAscTransactionReferenceAscIdAsc(
                        PaymentSource.EXTERNAL);
        List<ReconciliationResult> results = reconcile(batchId, internalRecords, externalRecords, startedAt);

        Instant completedAt = Instant.now();
        ReconciliationBatch completedBatch = new ReconciliationBatch(
                batchId, ReconciliationBatchStatus.COMPLETED, startedAt, completedAt, initiatedBy);
        batchRepository.save(completedBatch);
        List<ReconciliationResult> savedResults = resultRepository.saveAll(results);
        createExceptions(savedResults);
        return ReconciliationBatchResponse.from(completedBatch, savedResults);
    }

    @Transactional(readOnly = true)
    public ReconciliationBatchResponse get(UUID batchId) {
        ReconciliationBatch batch = findBatch(batchId);
        return ReconciliationBatchResponse.from(batch, resultsFor(batchId));
    }

    @Transactional(readOnly = true)
    public List<ReconciliationBatchResponse> list() {
        List<ReconciliationBatch> batches = batchRepository.findAllByOrderByStartedAtDesc();
        if (batches.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<ReconciliationResult>> resultsByBatch = resultRepository
                .findByBatchIdInOrderByCreatedAtAscIdAsc(
                        batches.stream().map(ReconciliationBatch::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(ReconciliationResult::getBatchId));
        return batches.stream()
                .map(batch -> ReconciliationBatchResponse.from(
                        batch, resultsByBatch.getOrDefault(batch.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReconciliationResultResponse> results(UUID batchId) {
        findBatch(batchId);
        return resultsFor(batchId).stream()
                .map(ReconciliationResultResponse::from)
                .toList();
    }

    private ReconciliationBatch findBatch(UUID batchId) {
        return batchRepository.findById(batchId)
                .orElseThrow(() -> new ReconciliationBatchNotFoundException(batchId));
    }

    private List<ReconciliationResult> resultsFor(UUID batchId) {
        return resultRepository.findByBatchIdOrderByCreatedAtAscIdAsc(batchId);
    }

    private List<ReconciliationResult> reconcile(
            UUID batchId,
            List<PaymentRecord> internalRecords,
            List<PaymentRecord> externalRecords,
            Instant createdAt) {
        Set<UUID> matchedInternalIds = new HashSet<>();
        Set<UUID> matchedExternalIds = new HashSet<>();
        List<ReconciliationResult> results = new ArrayList<>();

        matchUniqueKeys(
                batchId,
                internalRecords,
                externalRecords,
                PaymentRecord::getTransactionReference,
                "transactionReference",
                matchedInternalIds,
                matchedExternalIds,
                results,
                createdAt);
        matchUniqueKeys(
                batchId,
                internalRecords,
                externalRecords,
                PaymentRecord::getEndToEndId,
                "endToEndId",
                matchedInternalIds,
                matchedExternalIds,
                results,
                createdAt);
        matchUniqueKeys(
                batchId,
                internalRecords,
                externalRecords,
                record -> record.getAmount().stripTrailingZeros().toPlainString()
                        + "|" + record.getCurrency()
                        + "|" + record.getValueDate()
                        + "|" + normalize(record.getCounterparty()),
                "amount,currency,valueDate,counterparty",
                matchedInternalIds,
                matchedExternalIds,
                results,
                createdAt);

        internalRecords.stream()
                .filter(record -> !matchedInternalIds.contains(record.getId()))
                .forEach(record -> results.add(unmatchedResult(batchId, record, createdAt)));
        externalRecords.stream()
                .filter(record -> !matchedExternalIds.contains(record.getId()))
                .forEach(record -> results.add(unmatchedResult(batchId, record, createdAt)));
        return results;
    }

    private void matchUniqueKeys(
            UUID batchId,
            List<PaymentRecord> internalRecords,
            List<PaymentRecord> externalRecords,
            Function<PaymentRecord, String> keyFunction,
            String matchingRule,
            Set<UUID> matchedInternalIds,
            Set<UUID> matchedExternalIds,
            List<ReconciliationResult> results,
            Instant createdAt) {
        Map<String, List<PaymentRecord>> internalByKey = groupByKey(
                internalRecords, matchedInternalIds, keyFunction);
        Map<String, List<PaymentRecord>> externalByKey = groupByKey(
                externalRecords, matchedExternalIds, keyFunction);

        internalByKey.forEach((key, internalGroup) -> {
            List<PaymentRecord> externalGroup = externalByKey.get(key);
            if (internalGroup.size() != 1 || externalGroup == null || externalGroup.size() != 1) {
                return;
            }
            PaymentRecord internal = internalGroup.getFirst();
            PaymentRecord external = externalGroup.getFirst();
            matchedInternalIds.add(internal.getId());
            matchedExternalIds.add(external.getId());
            results.add(pairResult(batchId, internal, external, matchingRule, createdAt));
        });
    }

    private Map<String, List<PaymentRecord>> groupByKey(
            List<PaymentRecord> records,
            Set<UUID> alreadyMatched,
            Function<PaymentRecord, String> keyFunction) {
        Map<String, List<PaymentRecord>> grouped = new LinkedHashMap<>();
        for (PaymentRecord record : records) {
            if (alreadyMatched.contains(record.getId())) {
                continue;
            }
            String key = keyFunction.apply(record);
            if (key != null && !key.isBlank()) {
                grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(record);
            }
        }
        return grouped;
    }

    private ReconciliationResult pairResult(
            UUID batchId,
            PaymentRecord internal,
            PaymentRecord external,
            String matchingRule,
            Instant createdAt) {
        List<String> differences = new ArrayList<>();
        if (internal.getAmount().compareTo(external.getAmount()) != 0) {
            differences.add("amount");
        }
        if (!internal.getCurrency().equals(external.getCurrency())) {
            differences.add("currency");
        }
        if (!internal.getValueDate().equals(external.getValueDate())) {
            differences.add("valueDate");
        }
        if (!normalize(internal.getCounterparty()).equals(normalize(external.getCounterparty()))) {
            differences.add("counterparty");
        }
        if (internal.getStatus() != external.getStatus()
                || isFailed(internal.getStatus())
                || isFailed(external.getStatus())) {
            differences.add("paymentStatus");
        }

        ReconciliationResultStatus status = differences.isEmpty()
                ? ReconciliationResultStatus.MATCHED
                : ReconciliationResultStatus.DISCREPANCY;
        String evidence = "matchedBy=%s; differences=%s".formatted(
                matchingRule,
                differences.isEmpty() ? "none" : String.join(",", differences));
        return new ReconciliationResult(
                UUID.randomUUID(),
                batchId,
                internal.getId(),
                external.getId(),
                status,
                evidence,
                createdAt);
    }

    private ReconciliationResult unmatchedResult(UUID batchId, PaymentRecord record, Instant createdAt) {
        UUID internalId = record.getSource() == PaymentSource.INTERNAL ? record.getId() : null;
        UUID externalId = record.getSource() == PaymentSource.EXTERNAL ? record.getId() : null;
        return new ReconciliationResult(
                UUID.randomUUID(),
                batchId,
                internalId,
                externalId,
                ReconciliationResultStatus.UNMATCHED,
                "No unique counterpart found using transactionReference, endToEndId, "
                        + "or amount,currency,valueDate,counterparty.",
                createdAt);
    }

    private boolean isFailed(PaymentStatus status) {
        return status == PaymentStatus.FAILED || status == PaymentStatus.REJECTED;
    }

    private void createExceptions(List<ReconciliationResult> results) {
        Instant createdAt = Instant.now();
        List<ReconciliationResult> candidates = results.stream()
                .filter(result -> result.getStatus() != ReconciliationResultStatus.MATCHED)
                .toList();
        Set<UUID> recordIds = candidates.stream()
                .flatMap(result -> java.util.stream.Stream.of(
                        result.getInternalRecordId(), result.getExternalRecordId()))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (recordIds.isEmpty()) {
            return;
        }
        List<ReconciliationResult> priorResults = resultRepository
                .findByInternalRecordIdInOrExternalRecordIdIn(recordIds, recordIds);
        Set<UUID> priorExceptionResultIds = exceptionRepository.findByReconciliationResultIdIn(
                        priorResults.stream().map(ReconciliationResult::getId).toList())
                .stream()
                .map(PaymentException::getReconciliationResultId)
                .collect(Collectors.toSet());
        Set<UUID> previouslyExceptionRecordIds = priorResults.stream()
                .filter(result -> priorExceptionResultIds.contains(result.getId()))
                .flatMap(result -> java.util.stream.Stream.of(
                        result.getInternalRecordId(), result.getExternalRecordId()))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        List<PaymentException> exceptions = candidates.stream()
                .filter(result -> !previouslyExceptionRecordIds.contains(result.getInternalRecordId())
                        && !previouslyExceptionRecordIds.contains(result.getExternalRecordId()))
                .map(result -> {
                    PaymentExceptionCategory category = categoryFor(result);
                    ExceptionSeverity severity = severityFor(result, category);
                    return new PaymentException(
                            UUID.randomUUID(),
                            result.getId(),
                            category,
                            severity,
                            priorityFor(severity),
                            PaymentExceptionStatus.OPEN,
                            null,
                            createdAt);
                })
                .toList();
        exceptionRepository.saveAll(exceptions);
        auditLogRepository.saveAll(exceptions.stream()
                .map(exception -> new AuditLog(
                        UUID.randomUUID(),
                        "system",
                        "EXCEPTION_CREATED",
                        "PAYMENT_EXCEPTION",
                        exception.getId(),
                        "Created from reconciliation result %s; category=%s; severity=%s".formatted(
                                exception.getReconciliationResultId(),
                                exception.getCategory(),
                                exception.getSeverity()),
                        createdAt))
                .toList());
    }

    private PaymentExceptionCategory categoryFor(ReconciliationResult result) {
        if (result.getStatus() == ReconciliationResultStatus.UNMATCHED) {
            return PaymentExceptionCategory.UNMATCHED_TRANSACTION;
        }
        String evidence = result.getMatchingEvidence();
        if (evidence.contains("paymentStatus")) {
            return PaymentExceptionCategory.PAYMENT_FAILURE;
        }
        if (evidence.contains("amount")) {
            return PaymentExceptionCategory.AMOUNT_MISMATCH;
        }
        if (evidence.contains("currency")) {
            return PaymentExceptionCategory.CURRENCY_MISMATCH;
        }
        return PaymentExceptionCategory.SETTLEMENT_DISCREPANCY;
    }

    private ExceptionSeverity severityFor(ReconciliationResult result, PaymentExceptionCategory category) {
        if (category == PaymentExceptionCategory.PAYMENT_FAILURE
                || category == PaymentExceptionCategory.AMOUNT_MISMATCH
                || category == PaymentExceptionCategory.CURRENCY_MISMATCH) {
            return ExceptionSeverity.HIGH;
        }
        return result.getStatus() == ReconciliationResultStatus.UNMATCHED
                ? ExceptionSeverity.MEDIUM
                : ExceptionSeverity.LOW;
    }

    private int priorityFor(ExceptionSeverity severity) {
        return switch (severity) {
            case CRITICAL -> 1;
            case HIGH -> 2;
            case MEDIUM -> 3;
            case LOW -> 4;
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }
}
