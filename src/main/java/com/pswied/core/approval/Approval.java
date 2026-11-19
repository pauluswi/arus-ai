package com.pswied.core.approval;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approval")
public class Approval {

    @Id
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 40)
    private ApprovalTargetType targetType;

    @NotNull
    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ApprovalDecision decision;

    @NotBlank
    @Size(max = 120)
    @Column(name = "decided_by", nullable = false, length = 120)
    private String decidedBy;

    @NotBlank
    @Size(max = 1000)
    @Column(nullable = false, length = 1000)
    private String justification;

    @NotNull
    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;

    protected Approval() {
    }

    public Approval(
            UUID id,
            ApprovalTargetType targetType,
            UUID targetId,
            ApprovalDecision decision,
            String decidedBy,
            String justification,
            Instant decidedAt) {
        this.id = id;
        this.targetType = targetType;
        this.targetId = targetId;
        this.decision = decision;
        this.decidedBy = decidedBy;
        this.justification = justification;
        this.decidedAt = decidedAt;
    }

    public UUID getId() {
        return id;
    }

    public ApprovalTargetType getTargetType() {
        return targetType;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public ApprovalDecision getDecision() {
        return decision;
    }

    public String getDecidedBy() {
        return decidedBy;
    }

    public String getJustification() {
        return justification;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
}
