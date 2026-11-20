package com.pswied.core.reconciliation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReconciliationBatchResponse(
        UUID id,
        ReconciliationBatchStatus status,
        Instant startedAt,
        Instant completedAt,
        String initiatedBy,
        int resultCount,
        int matchedCount,
        int discrepancyCount,
        int unmatchedCount) {

    public static ReconciliationBatchResponse from(
            ReconciliationBatch batch,
            List<ReconciliationResult> results) {
        return new ReconciliationBatchResponse(
                batch.getId(),
                batch.getStatus(),
                batch.getStartedAt(),
                batch.getCompletedAt(),
                batch.getInitiatedBy(),
                results.size(),
                count(results, ReconciliationResultStatus.MATCHED),
                count(results, ReconciliationResultStatus.DISCREPANCY),
                count(results, ReconciliationResultStatus.UNMATCHED));
    }

    private static int count(List<ReconciliationResult> results, ReconciliationResultStatus status) {
        return (int) results.stream().filter(result -> result.getStatus() == status).count();
    }
}
