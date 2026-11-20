package com.pswied.core.payment;

public record PaymentImportResult(PaymentImportResponse response, boolean replayed) {
}
