package com.pswied.core.reconciliation;

import java.util.UUID;

public class ReconciliationBatchNotFoundException extends RuntimeException {

    public ReconciliationBatchNotFoundException(UUID batchId) {
        super("Reconciliation batch %s was not found.".formatted(batchId));
    }
}
