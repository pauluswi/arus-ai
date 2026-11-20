package com.pswied.core.exception;

import java.time.Instant;
import java.util.UUID;

public record PaymentExceptionResponse(
        UUID id,
        UUID reconciliationResultId,
        PaymentExceptionCategory category,
        ExceptionSeverity severity,
        int priority,
        PaymentExceptionStatus status,
        String assignedTo,
        Instant createdAt) {

    public static PaymentExceptionResponse from(PaymentException exception) {
        return new PaymentExceptionResponse(
                exception.getId(),
                exception.getReconciliationResultId(),
                exception.getCategory(),
                exception.getSeverity(),
                exception.getPriority(),
                exception.getStatus(),
                exception.getAssignedTo(),
                exception.getCreatedAt());
    }
}
