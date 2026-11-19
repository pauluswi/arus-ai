package com.pswied.core.network;

import com.pswied.core.ArusAiApplication;
import com.pswied.core.payment.PaymentRecord;
import com.pswied.core.payment.PaymentRecordRepository;
import com.pswied.core.payment.PaymentSource;
import com.pswied.core.payment.PaymentStatus;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ArusAiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentNetworkSubmissionTest {

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRecordRepository paymentRecordRepository;

    @Autowired
    private PaymentNetworkSubmissionCache submissionCache;

    @Test
    void acceptsPaymentAndCompletesNetworkSubmissionAsynchronously() throws Exception {
        PaymentRecord payment = paymentRecordRepository.saveAndFlush(payment("SUBMIT-ACCEPT", PaymentStatus.RECEIVED));

        String submission = mockMvc.perform(post(
                        "/api/v1/payments/{paymentRecordId}/network-submissions", payment.getId()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID submissionId = UUID.fromString(objectMapper.readTree(submission).get("submissionId").asText());

        Awaitility.await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                assertThat(submissionCache.get(submissionId).status())
                        .isEqualTo(NetworkSubmissionStatus.SUCCEEDED));

        mockMvc.perform(get("/api/v1/network-submissions/{submissionId}", submissionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.responseCode").value("ACCP"))
                .andExpect(jsonPath("$.result.networkReference").value("SIM-INTERNAL-SUBMIT-ACCEPT"));
    }

    @Test
    void returnsRejectionForFailedPayment() throws Exception {
        PaymentRecord payment = paymentRecordRepository.saveAndFlush(payment("SUBMIT-REJECT", PaymentStatus.FAILED));

        String submission = mockMvc.perform(post(
                        "/api/v1/payments/{paymentRecordId}/network-submissions", payment.getId()))
                .andExpect(status().isAccepted())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID submissionId = UUID.fromString(objectMapper.readTree(submission).get("submissionId").asText());

        Awaitility.await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                assertThat(submissionCache.get(submissionId).status())
                        .isEqualTo(NetworkSubmissionStatus.REJECTED));
    }

    @Test
    void rejectsSubmissionForUnknownPayment() throws Exception {
        mockMvc.perform(post(
                        "/api/v1/payments/{paymentRecordId}/network-submissions",
                        UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsNotFoundForUnknownSubmission() throws Exception {
        mockMvc.perform(get("/api/v1/network-submissions/{submissionId}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    private PaymentRecord payment(String reference, PaymentStatus status) {
        return new PaymentRecord(
                UUID.randomUUID(),
                PaymentSource.INTERNAL,
                reference,
                "E2E-" + reference,
                new BigDecimal("45.00"),
                "EUR",
                "Simulator Test",
                LocalDate.parse("2026-11-18"),
                status,
                Instant.parse("2026-11-18T09:00:00Z"),
                false);
    }
}
