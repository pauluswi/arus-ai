package com.pswied.core.payment;

public class DuplicatePaymentRecordException extends RuntimeException {

    public DuplicatePaymentRecordException(PaymentSource source, String transactionReference) {
        super("Payment record already exists for source %s and reference %s"
                .formatted(source, transactionReference));
    }
}
