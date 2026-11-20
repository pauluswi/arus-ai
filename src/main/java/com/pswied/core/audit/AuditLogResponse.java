package com.pswied.core.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        String actor,
        String action,
        String aggregateType,
        UUID aggregateId,
        String details,
        Instant occurredAt) {

    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getActor(),
                log.getAction(),
                log.getAggregateType(),
                log.getAggregateId(),
                log.getDetails(),
                log.getOccurredAt());
    }
}
