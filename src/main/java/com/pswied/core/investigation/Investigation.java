package com.pswied.core.investigation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "investigation")
public class Investigation {

    @Id
    private UUID id;

    @Column(name = "exception_id", nullable = false, unique = true)
    private UUID exceptionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private InvestigationStatus status;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    protected Investigation() {
    }

    public Investigation(
            UUID id,
            UUID exceptionId,
            InvestigationStatus status,
            String summary,
            Instant openedAt,
            Instant closedAt) {
        this.id = id;
        this.exceptionId = exceptionId;
        this.status = status;
        this.summary = summary;
        this.openedAt = openedAt;
        this.closedAt = closedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getExceptionId() {
        return exceptionId;
    }

    public InvestigationStatus getStatus() {
        return status;
    }

    public String getSummary() {
        return summary;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }
}
