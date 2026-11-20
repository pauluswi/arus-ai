package com.pswied.core.payment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PaymentRecordsIngestedEvent(
        UUID importId,
        List<UUID> paymentRecordIds,
        Instant occurredAt) {

    public PaymentRecordsIngestedEvent {
        paymentRecordIds = List.copyOf(paymentRecordIds);
    }
}
