package com.pswied.core.investigation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "investigation_note")
public class InvestigationNote {

    @Id
    private UUID id;

    @Column(name = "exception_id", nullable = false)
    private UUID exceptionId;

    @Column(nullable = false, length = 120)
    private String author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected InvestigationNote() {
    }

    public InvestigationNote(UUID id, UUID exceptionId, String author, String note, Instant createdAt) {
        this.id = id;
        this.exceptionId = exceptionId;
        this.author = author;
        this.note = note;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getExceptionId() {
        return exceptionId;
    }

    public String getAuthor() {
        return author;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
