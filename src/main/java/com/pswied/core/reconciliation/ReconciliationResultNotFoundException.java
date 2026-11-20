package com.pswied.core.reconciliation;

import java.util.UUID;

public class ReconciliationResultNotFoundException extends RuntimeException {

    public ReconciliationResultNotFoundException(UUID resultId) {
        super("Reconciliation result %s was not found.".formatted(resultId));
    }
}
