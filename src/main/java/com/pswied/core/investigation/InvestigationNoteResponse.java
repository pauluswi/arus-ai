package com.pswied.core.investigation;

import java.time.Instant;
import java.util.UUID;

public record InvestigationNoteResponse(
        UUID id,
        UUID exceptionId,
        String author,
        String note,
        Instant createdAt) {

    public static InvestigationNoteResponse from(InvestigationNote note) {
        return new InvestigationNoteResponse(
                note.getId(),
                note.getExceptionId(),
                note.getAuthor(),
                note.getNote(),
                note.getCreatedAt());
    }
}
