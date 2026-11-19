package com.pswied.core.payment;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentRecordRequest(
        @NotNull PaymentSource source,
        @NotBlank @Size(max = 100) String transactionReference,
        @Size(max = 100) String endToEndId,
        @NotNull @Positive @Digits(integer = 15, fraction = 4) BigDecimal amount,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
        @NotBlank @Size(max = 140) String counterparty,
        @NotNull LocalDate valueDate,
        @NotNull PaymentStatus status) {
}
