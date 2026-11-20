package com.pswied.core.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PaymentImportRequest(
        @NotEmpty
        @Size(max = PaymentImportService.MAX_RECORDS)
        List<@NotNull @Valid PaymentRecordRequest> records) {
}
