package com.pswied.core.payment;

import java.util.UUID;

public class PaymentRecordNotFoundException extends RuntimeException {

    public PaymentRecordNotFoundException(UUID id) {
        super("Payment record not found: " + id);
    }
}
