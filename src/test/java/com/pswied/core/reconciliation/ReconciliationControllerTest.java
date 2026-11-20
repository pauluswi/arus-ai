package com.pswied.core.reconciliation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pswied.core.ArusAiApplication;
import com.pswied.core.payment.PaymentRecordResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ArusAiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReconciliationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void reconcilesExactCompositeDiscrepantAndUnmatchedRecords() throws Exception {
        String suffix = UUID.randomUUID().toString();
        PaymentRecordResponse exactInternal = createPayment(
                "INTERNAL", "REF-" + suffix, "E2E-EXACT-" + suffix, "100.00", "USD", "Example Bank", "SETTLED");
        PaymentRecordResponse exactExternal = createPayment(
                "EXTERNAL", "REF-" + suffix, "E2E-EXACT-" + suffix, "100.00", "USD", "Example Bank", "SETTLED");
        PaymentRecordResponse discrepancyInternal = createPayment(
                "INTERNAL", "INT-DIFF-" + suffix, "E2E-DIFF-" + suffix, "120.00", "EUR", "Other Bank", "FAILED");
        PaymentRecordResponse discrepancyExternal = createPayment(
                "EXTERNAL", "EXT-DIFF-" + suffix, "E2E-DIFF-" + suffix, "110.00", "EUR", "Other Bank", "REJECTED");
        PaymentRecordResponse compositeInternal = createPayment(
                "INTERNAL", "INT-COMPOSITE-" + suffix, "E2E-INT-" + suffix, "50.00", "GBP", "Composite Bank", "SETTLED");
        PaymentRecordResponse compositeExternal = createPayment(
                "EXTERNAL", "EXT-COMPOSITE-" + suffix, "E2E-EXT-" + suffix, "50.00", "GBP", "composite bank", "SETTLED");
        PaymentRecordResponse unmatchedInternal = createPayment(
                "INTERNAL", "INT-UNMATCHED-" + suffix, "E2E-UNMATCHED-INT-" + suffix,
                "31.00", "EUR", "Internal Only", "RECEIVED");
        PaymentRecordResponse unmatchedExternal = createPayment(
                "EXTERNAL", "EXT-UNMATCHED-" + suffix, "E2E-UNMATCHED-EXT-" + suffix,
                "32.00", "EUR", "External Only", "RECEIVED");

        var reconciler = SecurityMockMvcRequestPostProcessors.user("reconciler")
                .roles("RECONCILIATION_OFFICER");
        String batchBody = mockMvc.perform(post("/api/v1/reconciliation/batches").with(reconciler))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        ReconciliationBatchResponse batch = objectMapper.readValue(batchBody, ReconciliationBatchResponse.class);

        assertThat(batch.status()).isEqualTo(ReconciliationBatchStatus.COMPLETED);
        assertThat(batch.matchedCount()).isGreaterThanOrEqualTo(2);
        assertThat(batch.discrepancyCount()).isGreaterThanOrEqualTo(1);
        assertThat(batch.unmatchedCount()).isGreaterThanOrEqualTo(2);

        String resultsBody = mockMvc.perform(get("/api/v1/reconciliation/batches/{id}/results", batch.id())
                        .with(reconciler))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<ReconciliationResultResponse> results = objectMapper.readValue(
                resultsBody, new TypeReference<>() {});

        assertResult(results, exactInternal, exactExternal, ReconciliationResultStatus.MATCHED, "transactionReference");
        assertResult(results, discrepancyInternal, discrepancyExternal, ReconciliationResultStatus.DISCREPANCY, "endToEndId");
        assertThat(results).anySatisfy(result -> {
            assertThat(result.internalRecordId()).isEqualTo(discrepancyInternal.id());
            assertThat(result.externalRecordId()).isEqualTo(discrepancyExternal.id());
            assertThat(result.matchingEvidence()).contains("differences=amount,paymentStatus");
        });
        assertResult(results, compositeInternal, compositeExternal, ReconciliationResultStatus.MATCHED,
                "amount,currency,valueDate,counterparty");
        assertResult(results, unmatchedInternal, null, ReconciliationResultStatus.UNMATCHED, null);
        assertResult(results, null, unmatchedExternal, ReconciliationResultStatus.UNMATCHED, null);

        mockMvc.perform(get("/api/v1/reconciliation/batches/{id}", batch.id()).with(reconciler))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/reconciliation/batches").with(reconciler))
                .andExpect(status().isOk());
    }

    @Test
    void returnsNotFoundForUnknownBatch() throws Exception {
        mockMvc.perform(get("/api/v1/reconciliation/batches/{id}", UUID.randomUUID())
                        .with(SecurityMockMvcRequestPostProcessors.user("reconciler")
                                .roles("RECONCILIATION_OFFICER")))
                .andExpect(status().isNotFound());
    }

    private PaymentRecordResponse createPayment(
            String source,
            String reference,
            String endToEndId,
            String amount,
            String currency,
            String counterparty,
            String status) throws Exception {
        String body = """
                {
                  "source": "%s",
                  "transactionReference": "%s",
                  "endToEndId": "%s",
                  "amount": %s,
                  "currency": "%s",
                  "counterparty": "%s",
                  "valueDate": "%s",
                  "status": "%s"
                }
                """.formatted(
                source,
                reference,
                endToEndId,
                new BigDecimal(amount).toPlainString(),
                currency,
                counterparty,
                LocalDate.parse("2026-11-18"),
                status);
        String response = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(response, PaymentRecordResponse.class);
    }

    private void assertResult(
            List<ReconciliationResultResponse> results,
            PaymentRecordResponse internal,
            PaymentRecordResponse external,
            ReconciliationResultStatus expectedStatus,
            String expectedEvidence) {
        assertThat(results)
                .anySatisfy(result -> {
                    if (internal != null) {
                        assertThat(result.internalRecordId()).isEqualTo(internal.id());
                    }
                    if (external != null) {
                        assertThat(result.externalRecordId()).isEqualTo(external.id());
                    }
                    assertThat(result.status()).isEqualTo(expectedStatus);
                    if (expectedEvidence != null) {
                        assertThat(result.matchingEvidence()).contains(expectedEvidence);
                    }
                });
    }
}
