package com.pswied.core.payment;

public class PaymentImportIdempotencyConflictException extends RuntimeException {

    public PaymentImportIdempotencyConflictException(String idempotencyKey) {
        super("Idempotency-Key has already been used with a different payment import: " + idempotencyKey);
    }
}
