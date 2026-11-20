package com.pswied.core.payment;

import java.util.List;

public class PaymentImportValidationException extends RuntimeException {

    private final List<String> errors;

    public PaymentImportValidationException(List<String> errors) {
        super("The payment import contains invalid records.");
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
