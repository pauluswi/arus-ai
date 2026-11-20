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
@Table(name = "exception_resolution_request")
public class ExceptionResolutionRequest {

    @Id
    private UUID id;

    @Column(name = "exception_id", nullable = false)
    private UUID exceptionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ExceptionResolutionAction action;

    @Column(name = "requested_by", nullable = false, length = 120)
    private String requestedBy;

    @Column(nullable = false, length = 1000)
    private String justification;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_status", nullable = false, length = 24)
    private PaymentExceptionStatus returnStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ExceptionResolutionRequestStatus status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    protected ExceptionResolutionRequest() {
    }

    public ExceptionResolutionRequest(
            UUID id,
            UUID exceptionId,
            ExceptionResolutionAction action,
            String requestedBy,
            String justification,
            PaymentExceptionStatus returnStatus,
            ExceptionResolutionRequestStatus status,
            Instant requestedAt,
            Instant decidedAt) {
        this.id = id;
        this.exceptionId = exceptionId;
        this.action = action;
        this.requestedBy = requestedBy;
        this.justification = justification;
        this.returnStatus = returnStatus;
        this.status = status;
        this.requestedAt = requestedAt;
        this.decidedAt = decidedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getExceptionId() {
        return exceptionId;
    }

    public ExceptionResolutionAction getAction() {
        return action;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public String getJustification() {
        return justification;
    }

    public PaymentExceptionStatus getReturnStatus() {
        return returnStatus;
    }

    public ExceptionResolutionRequestStatus getStatus() {
        return status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void decide(ExceptionResolutionRequestStatus status, Instant decidedAt) {
        this.status = status;
        this.decidedAt = decidedAt;
    }
}
