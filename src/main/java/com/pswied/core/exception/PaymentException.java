package com.pswied.core.exception;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_exception")
public class PaymentException {

    @Id
    private UUID id;

    @Column(name = "reconciliation_result_id", nullable = false)
    private UUID reconciliationResultId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PaymentExceptionCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ExceptionSeverity severity;

    @Column(nullable = false)
    private int priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PaymentExceptionStatus status;

    @Column(name = "assigned_to", length = 120)
    private String assignedTo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PaymentException() {
    }

    public PaymentException(
            UUID id,
            UUID reconciliationResultId,
            PaymentExceptionCategory category,
            ExceptionSeverity severity,
            int priority,
            PaymentExceptionStatus status,
            String assignedTo,
            Instant createdAt) {
        this.id = id;
        this.reconciliationResultId = reconciliationResultId;
        this.category = category;
        this.severity = severity;
        this.priority = priority;
        this.status = status;
        this.assignedTo = assignedTo;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getReconciliationResultId() {
        return reconciliationResultId;
    }

    public PaymentExceptionCategory getCategory() {
        return category;
    }

    public ExceptionSeverity getSeverity() {
        return severity;
    }

    public int getPriority() {
        return priority;
    }

    public PaymentExceptionStatus getStatus() {
        return status;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
