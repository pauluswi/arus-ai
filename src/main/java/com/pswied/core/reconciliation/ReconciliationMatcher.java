package com.pswied.core.reconciliation;

import com.pswied.core.payment.PaymentRecord;
import com.pswied.core.payment.PaymentSource;
import com.pswied.core.payment.PaymentStatus;
import org.springframework.stereotype.Component;

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

@Component
public class ReconciliationMatcher {

    public List<ReconciliationResult> reconcile(
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

    private String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }
}
