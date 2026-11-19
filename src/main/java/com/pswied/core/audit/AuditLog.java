package com.pswied.core.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    private UUID id;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String actor;

    @NotBlank
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String action;

    @NotBlank
    @Size(max = 40)
    @Column(name = "aggregate_type", nullable = false, length = 40)
    private String aggregateType;

    @NotNull
    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @NotNull
    @Column(nullable = false, columnDefinition = "TEXT")
    private String details;

    @NotNull
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AuditLog() {
    }

    public AuditLog(
            UUID id,
            String actor,
            String action,
            String aggregateType,
            UUID aggregateId,
            String details,
            Instant occurredAt) {
        this.id = id;
        this.actor = actor;
        this.action = action;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.details = details;
        this.occurredAt = occurredAt;
    }

    public UUID getId() {
        return id;
    }

    public String getActor() {
        return actor;
    }

    public String getAction() {
        return action;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getDetails() {
        return details;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
