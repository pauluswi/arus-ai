package com.pswied.core.reconciliation;

import com.pswied.core.payment.PaymentRecord;
import com.pswied.core.payment.PaymentRecordRepository;
import com.pswied.core.payment.PaymentSource;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReconciliationService {

    private final PaymentRecordRepository paymentRecordRepository;
    private final ReconciliationBatchRepository batchRepository;
    private final ReconciliationResultRepository resultRepository;
    private final PaymentExceptionRepository exceptionRepository;
    private final AuditLogRepository auditLogRepository;
    private final ReconciliationMatcher reconciliationMatcher;

    public ReconciliationService(
            PaymentRecordRepository paymentRecordRepository,
            ReconciliationBatchRepository batchRepository,
            ReconciliationResultRepository resultRepository,
            PaymentExceptionRepository exceptionRepository,
            AuditLogRepository auditLogRepository,
            ReconciliationMatcher reconciliationMatcher) {
        this.paymentRecordRepository = paymentRecordRepository;
        this.batchRepository = batchRepository;
        this.resultRepository = resultRepository;
        this.exceptionRepository = exceptionRepository;
        this.auditLogRepository = auditLogRepository;
        this.reconciliationMatcher = reconciliationMatcher;
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
        List<ReconciliationResult> results = reconciliationMatcher.reconcile(
                batchId, internalRecords, externalRecords, startedAt);

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

}
