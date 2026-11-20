package com.pswied.core.payment;

import java.util.List;
import java.util.UUID;

public record PaymentImportResponse(
        UUID importId,
        int totalRows,
        int createdCount,
        int duplicateCount,
        List<PaymentImportItemResult> results) {

    public PaymentImportResponse {
        results = List.copyOf(results);
    }
}
