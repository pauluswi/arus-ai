package com.pswied.core.payment;

public record PaymentImportItemResult(
        int rowNumber,
        PaymentImportOutcome outcome,
        PaymentRecordResponse payment,
        String message) {
}
