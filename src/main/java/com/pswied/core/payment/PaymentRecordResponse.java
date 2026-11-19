package com.pswied.core.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentRecordResponse(
        UUID id,
        PaymentSource source,
        String transactionReference,
        String endToEndId,
        BigDecimal amount,
        String currency,
        String counterparty,
        LocalDate valueDate,
        PaymentStatus status,
        Instant createdAt) {

    public static PaymentRecordResponse from(PaymentRecord record) {
        return new PaymentRecordResponse(
                record.getId(),
                record.getSource(),
                record.getTransactionReference(),
                record.getEndToEndId(),
                record.getAmount(),
                record.getCurrency(),
                record.getCounterparty(),
                record.getValueDate(),
                record.getStatus(),
                record.getCreatedAt());
    }
}
