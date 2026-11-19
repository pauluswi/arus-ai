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
@Table(name = "reconciliation_batch")
public class ReconciliationBatch {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ReconciliationBatchStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "initiated_by", nullable = false, length = 120)
    private String initiatedBy;

    protected ReconciliationBatch() {
    }

    public ReconciliationBatch(
            UUID id,
            ReconciliationBatchStatus status,
            Instant startedAt,
            Instant completedAt,
            String initiatedBy) {
        this.id = id;
        this.status = status;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.initiatedBy = initiatedBy;
    }

    public UUID getId() {
        return id;
    }

    public ReconciliationBatchStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getInitiatedBy() {
        return initiatedBy;
    }
}
