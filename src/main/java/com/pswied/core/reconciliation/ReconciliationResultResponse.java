package com.pswied.core.reconciliation;

import java.time.Instant;
import java.util.UUID;

public record ReconciliationResultResponse(
        UUID id,
        UUID batchId,
        UUID internalRecordId,
        UUID externalRecordId,
        ReconciliationResultStatus status,
        String matchingEvidence,
        Instant createdAt) {

    public static ReconciliationResultResponse from(ReconciliationResult result) {
        return new ReconciliationResultResponse(
                result.getId(),
                result.getBatchId(),
                result.getInternalRecordId(),
                result.getExternalRecordId(),
                result.getStatus(),
                result.getMatchingEvidence(),
                result.getCreatedAt());
    }
}
