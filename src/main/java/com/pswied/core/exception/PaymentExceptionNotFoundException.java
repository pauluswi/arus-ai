package com.pswied.core.exception;

import java.util.UUID;

public class PaymentExceptionNotFoundException extends RuntimeException {

    public PaymentExceptionNotFoundException(UUID exceptionId) {
        super("Payment exception %s was not found.".formatted(exceptionId));
    }
}
