package com.pswied.core.exception;

import java.time.Instant;
import java.util.UUID;

public record ExceptionResolutionRequestResponse(
        UUID id,
        UUID exceptionId,
        ExceptionResolutionAction action,
        String requestedBy,
        String justification,
        ExceptionResolutionRequestStatus status,
        Instant requestedAt,
        Instant decidedAt) {

    public static ExceptionResolutionRequestResponse from(ExceptionResolutionRequest request) {
        return new ExceptionResolutionRequestResponse(
                request.getId(),
                request.getExceptionId(),
                request.getAction(),
                request.getRequestedBy(),
                request.getJustification(),
                request.getStatus(),
                request.getRequestedAt(),
                request.getDecidedAt());
    }
}
