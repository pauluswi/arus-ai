package com.pswied.core.reconciliation;

import com.pswied.core.payment.PaymentRecord;
import com.pswied.core.payment.PaymentSource;
import com.pswied.core.payment.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReconciliationMatcherTest {

    private final ReconciliationMatcher matcher = new ReconciliationMatcher();
    private final UUID batchId = UUID.randomUUID();
    private final Instant createdAt = Instant.parse("2026-11-20T00:00:00Z");

    @Test
    void matchesOnTransactionReferenceBeforeOtherKeys() {
        PaymentRecord internal = record(PaymentSource.INTERNAL, "REF-1", "E2E-1", "12.00", "Counterparty");
        PaymentRecord external = record(PaymentSource.EXTERNAL, "REF-1", "E2E-OTHER", "13.00", "Other");

        List<ReconciliationResult> results = matcher.reconcile(
                batchId, List.of(internal), List.of(external), createdAt);

        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.getStatus()).isEqualTo(ReconciliationResultStatus.DISCREPANCY);
            assertThat(result.getMatchingEvidence())
                    .isEqualTo("matchedBy=transactionReference; differences=amount,counterparty");
        });
    }

    @Test
    void matchesOnEndToEndIdWhenReferencesDifferAndMarksFailedPaymentsAsDiscrepant() {
        PaymentRecord internal = record(
                PaymentSource.INTERNAL, "INT-1", "E2E-1", "12.00", "Counterparty", PaymentStatus.FAILED);
        PaymentRecord external = record(
                PaymentSource.EXTERNAL, "EXT-1", "E2E-1", "12.00", "Counterparty", PaymentStatus.SETTLED);

        List<ReconciliationResult> results = matcher.reconcile(
                batchId, List.of(internal), List.of(external), createdAt);

        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.getStatus()).isEqualTo(ReconciliationResultStatus.DISCREPANCY);
            assertThat(result.getMatchingEvidence())
                    .isEqualTo("matchedBy=endToEndId; differences=paymentStatus");
        });
    }

    @Test
    void matchesNormalizedCompositeValuesAndLeavesAmbiguousKeysUnmatched() {
        PaymentRecord compositeInternal = record(
                PaymentSource.INTERNAL, "INT-C", null, "12.000", "  Example Bank ");
        PaymentRecord compositeExternal = record(
                PaymentSource.EXTERNAL, "EXT-C", null, "12.00", "example bank");
        PaymentRecord ambiguousInternalOne = record(
                PaymentSource.INTERNAL, "INT-A1", "E2E-A1", "20.00", "Ambiguous");
        PaymentRecord ambiguousInternalTwo = record(
                PaymentSource.INTERNAL, "INT-A2", "E2E-A2", "20.00", "Ambiguous");
        PaymentRecord ambiguousExternal = record(
                PaymentSource.EXTERNAL, "EXT-A", "E2E-A3", "20.00", "Ambiguous");

        List<ReconciliationResult> results = matcher.reconcile(
                batchId,
                List.of(compositeInternal, ambiguousInternalOne, ambiguousInternalTwo),
                List.of(compositeExternal, ambiguousExternal),
                createdAt);

        assertThat(results).hasSize(4);
        assertThat(results).filteredOn(result -> result.getStatus() == ReconciliationResultStatus.MATCHED)
                .singleElement()
                .satisfies(result -> assertThat(result.getMatchingEvidence())
                        .contains("matchedBy=amount,currency,valueDate,counterparty"));
        assertThat(results).filteredOn(result -> result.getStatus() == ReconciliationResultStatus.UNMATCHED)
                .hasSize(3);
    }

    private PaymentRecord record(
            PaymentSource source,
            String reference,
            String endToEndId,
            String amount,
            String counterparty) {
        return record(source, reference, endToEndId, amount, counterparty, PaymentStatus.SETTLED);
    }

    private PaymentRecord record(
            PaymentSource source,
            String reference,
            String endToEndId,
            String amount,
            String counterparty,
            PaymentStatus status) {
        return new PaymentRecord(
                UUID.randomUUID(),
                source,
                reference,
                endToEndId,
                new BigDecimal(amount),
                "EUR",
                counterparty,
                LocalDate.parse("2026-11-18"),
                status,
                createdAt,
                false);
    }
}
