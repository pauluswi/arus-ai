package com.pswied.core.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pswied.core.ArusAiApplication;
import com.pswied.core.payment.PaymentRecordResponse;
import com.pswied.core.payment.PaymentSource;
import com.pswied.core.payment.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ArusAiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentOperationsAiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void exposesDeterministicPaymentAiCapabilities() throws Exception {
        String unique = UUID.randomUUID().toString();
        PaymentRecordResponse internal = createPayment(
                PaymentSource.INTERNAL, "INT-" + unique, "E2E-" + unique,
                "12000.00", PaymentStatus.FAILED);
        PaymentRecordResponse external = createPayment(
                PaymentSource.EXTERNAL, "NET-" + unique, "E2E-" + unique,
                "12000.00", PaymentStatus.RECEIVED);
        UserDetails operator = User.withUsername("ai-operator")
                .password("not-used")
                .roles("PAYMENT_OPERATOR")
                .build();

        JsonNode ranking = getJson(
                "/api/v1/ai/payments/" + internal.id() + "/match-ranking", operator);
        assertThat(ranking).hasSize(1);
        assertThat(ranking.get(0).get("paymentRecordId").asText()).isEqualTo(external.id().toString());
        assertThat(ranking.get(0).get("score").decimalValue()).isEqualByComparingTo("0.60");
        assertThat(ranking.get(0).get("matchingSignals").toString())
                .contains("endToEndId", "amount", "currency");

        JsonNode anomaly = getJson(
                "/api/v1/ai/payments/" + internal.id() + "/anomalies", operator);
        assertThat(anomaly.get("severity").asText()).isEqualTo("HIGH");
        assertThat(anomaly.get("score").decimalValue()).isEqualByComparingTo("1.00");
        assertThat(anomaly.get("disclaimer").asText()).contains("not a calibrated model prediction");

        JsonNode failure = getJson(
                "/api/v1/ai/payments/" + internal.id() + "/failure-prediction", operator);
        assertThat(failure.get("risk").asText()).isEqualTo("HIGH");
        assertThat(failure.get("score").decimalValue()).isEqualByComparingTo("0.94");

        JsonNode knowledge = getJson("/api/v1/ai/knowledge?query=approval", operator);
        assertThat(knowledge.get("provider").asText()).isEqualTo("mock-knowledge-fixtures");
        assertThat(knowledge.get("results").get(0).get("id").asText()).isEqualTo("exception-approval");
    }

    @Test
    void classifiesPersistedReconciliationEvidenceAndProtectsAiEndpoints() throws Exception {
        String unique = UUID.randomUUID().toString();
        PaymentRecordResponse internal = createPayment(
                PaymentSource.INTERNAL, "INT-" + unique, "E2E-" + unique,
                "125.00", PaymentStatus.FAILED);
        PaymentRecordResponse external = createPayment(
                PaymentSource.EXTERNAL, "INT-" + unique, "E2E-" + unique,
                "125.00", PaymentStatus.SETTLED);
        var reconciler = user("ai-reconciler").roles("RECONCILIATION_OFFICER");
        String batchBody = mockMvc.perform(post("/api/v1/reconciliation/batches").with(reconciler))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID batchId = objectMapper.readTree(batchBody).get("id").traverse(objectMapper).readValueAs(UUID.class);
        JsonNode results = objectMapper.readTree(mockMvc.perform(
                        get("/api/v1/reconciliation/batches/{id}/results", batchId).with(reconciler))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID resultId = findResult(results, internal.id(), external.id());

        JsonNode classification = getJson(
                "/api/v1/ai/reconciliation-results/" + resultId + "/classification",
                User.withUsername("ai-reviewer")
                        .password("not-used")
                        .roles("AI_GOVERNANCE_OFFICER")
                        .build());
        assertThat(classification.get("category").asText()).isEqualTo("PAYMENT_FAILURE");
        assertThat(classification.get("confidence").decimalValue()).isEqualByComparingTo("0.94");

        mockMvc.perform(get("/api/v1/ai/payments/{id}/anomalies", internal.id())
                        .with(user("unauthorized").roles("SYSTEM_ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/ai/knowledge").param("query", " ")
                        .with(user("ai-operator").roles("PAYMENT_OPERATOR")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/ai/payments/{id}/failure-prediction", UUID.randomUUID())
                        .with(user("ai-operator").roles("PAYMENT_OPERATOR")))
                .andExpect(status().isNotFound());
    }

    @Test
    void doesNotClassifyMatchedReconciliationResultsAsExceptions() throws Exception {
        String unique = UUID.randomUUID().toString();
        PaymentRecordResponse internal = createPayment(
                PaymentSource.INTERNAL, "MATCH-" + unique, "MATCH-E2E-" + unique,
                "85.00", PaymentStatus.RECEIVED);
        PaymentRecordResponse external = createPayment(
                PaymentSource.EXTERNAL, "MATCH-" + unique, "MATCH-E2E-" + unique,
                "85.00", PaymentStatus.RECEIVED);
        var reconciler = user("ai-reconciler").roles("RECONCILIATION_OFFICER");
        String batchBody = mockMvc.perform(post("/api/v1/reconciliation/batches").with(reconciler))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID batchId = objectMapper.readTree(batchBody).get("id").traverse(objectMapper).readValueAs(UUID.class);
        JsonNode results = objectMapper.readTree(mockMvc.perform(
                        get("/api/v1/reconciliation/batches/{id}/results", batchId).with(reconciler))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        UUID resultId = findResult(results, internal.id(), external.id());

        mockMvc.perform(get("/api/v1/ai/reconciliation-results/{id}/classification", resultId)
                        .with(user("ai-operator").roles("PAYMENT_OPERATOR")))
                .andExpect(status().isBadRequest());
    }

    private PaymentRecordResponse createPayment(
            PaymentSource source,
            String reference,
            String endToEndId,
            String amount,
            PaymentStatus status) throws Exception {
        String body = """
                {
                  "source": "%s",
                  "transactionReference": "%s",
                  "endToEndId": "%s",
                  "amount": %s,
                  "currency": "EUR",
                  "counterparty": "Mock AI Test Bank",
                  "valueDate": "%s",
                  "status": "%s"
                }
                """.formatted(source, reference, endToEndId, amount, LocalDate.of(2026, 11, 18), status);
        String response = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(response, PaymentRecordResponse.class);
    }

    private JsonNode getJson(String path, UserDetails userDetails) throws Exception {
        MvcResult result = mockMvc.perform(get(path).with(user(userDetails)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private UUID findResult(JsonNode results, UUID internalId, UUID externalId) {
        for (JsonNode result : results) {
            if (internalId.toString().equals(result.path("internalRecordId").asText())
                    && externalId.toString().equals(result.path("externalRecordId").asText())) {
                return UUID.fromString(result.get("id").asText());
            }
        }
        throw new AssertionError("Expected reconciliation result for the created payment pair.");
    }
}
