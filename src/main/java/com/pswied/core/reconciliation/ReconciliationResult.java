package com.pswied.core.reconciliation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_result")
public class ReconciliationResult {

    @Id
    private UUID id;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "internal_record_id")
    private UUID internalRecordId;

    @Column(name = "external_record_id")
    private UUID externalRecordId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ReconciliationResultStatus status;

    @Column(name = "matching_evidence", nullable = false, columnDefinition = "TEXT")
    private String matchingEvidence;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ReconciliationResult() {
    }

    public ReconciliationResult(
            UUID id,
            UUID batchId,
            UUID internalRecordId,
            UUID externalRecordId,
            ReconciliationResultStatus status,
            String matchingEvidence,
            Instant createdAt) {
        this.id = id;
        this.batchId = batchId;
        this.internalRecordId = internalRecordId;
        this.externalRecordId = externalRecordId;
        this.status = status;
        this.matchingEvidence = matchingEvidence;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public UUID getInternalRecordId() {
        return internalRecordId;
    }

    public UUID getExternalRecordId() {
        return externalRecordId;
    }

    public ReconciliationResultStatus getStatus() {
        return status;
    }

    public String getMatchingEvidence() {
        return matchingEvidence;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
