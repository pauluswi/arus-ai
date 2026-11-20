package com.pswied.core.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pswied.core.ArusAiApplication;
import com.pswied.core.exception.PaymentExceptionResponse;
import com.pswied.core.investigation.InvestigationNoteResponse;
import com.pswied.core.payment.PaymentRecordResponse;
import com.pswied.core.reconciliation.ReconciliationResultResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ArusAiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InvestigationAiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void returnsDeterministicFixtureReportWithEvidenceAndConfidence() throws Exception {
        String reference = "ai-" + UUID.randomUUID();
        CreatedException createdException = createUnmatchedException(reference);
        UUID exceptionId = createdException.exceptionId();
        String noteResponse = mockMvc.perform(post("/api/v1/exceptions/{id}/notes", exceptionId)
                        .with(user("investigator").roles("PAYMENT_OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"note":"Checked the payment ledger; no corresponding external entry was found."}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        InvestigationNoteResponse note = objectMapper.readValue(noteResponse, InvestigationNoteResponse.class);

        var investigator = SecurityMockMvcRequestPostProcessors.user("investigator").roles("PAYMENT_OPERATOR");
        String firstResponse = mockMvc.perform(get(
                                "/api/v1/exceptions/{id}/investigation-report", exceptionId)
                        .with(investigator))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String replayedResponse = mockMvc.perform(get(
                                "/api/v1/exceptions/{id}/investigation-report", exceptionId)
                        .with(investigator))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(replayedResponse).isEqualTo(firstResponse);
        InvestigationReport report = objectMapper.readValue(firstResponse, InvestigationReport.class);
        assertThat(report.provider()).isEqualTo("mock-fixture");
        assertThat(report.fixtureId()).isEqualTo("unmatched-transaction-v1");
        assertThat(report.category()).isEqualTo(com.pswied.core.exception.PaymentExceptionCategory.UNMATCHED_TRANSACTION);
        assertThat(report.confidence().score()).isEqualByComparingTo("0.82");
        assertThat(report.confidence().label()).isEqualTo("MEDIUM");
        assertThat(report.recommendations()).hasSize(2);
        assertThat(report.recommendations().getFirst().confidence().score()).isEqualByComparingTo("0.79");
        assertThat(report.recommendations().getFirst().evidenceReferences())
                .contains("PAYMENT_EXCEPTION:" + exceptionId, "INVESTIGATION_NOTE:" + note.id());
        assertThat(report.evidenceReferences())
                .anySatisfy(evidenceReference -> {
                    assertThat(evidenceReference.type()).isEqualTo(InvestigationEvidenceType.INVESTIGATION_NOTE);
                    assertThat(evidenceReference.id()).isEqualTo(note.id());
                    assertThat(evidenceReference.description()).contains("no corresponding external entry");
                })
                .anySatisfy(evidenceReference -> {
                    assertThat(evidenceReference.type()).isEqualTo(InvestigationEvidenceType.PAYMENT_RECORD);
                    assertThat(evidenceReference.id()).isEqualTo(createdException.paymentRecordId());
                    assertThat(evidenceReference.description()).contains(reference);
                });
        assertThat(report.disclaimer()).contains("require human review");
    }

    @Test
    void protectsReportEndpointAndReturnsNotFoundForUnknownException() throws Exception {
        mockMvc.perform(get("/api/v1/exceptions/{id}/investigation-report", UUID.randomUUID())
                        .with(user("administrator").roles("SYSTEM_ADMIN")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/exceptions/{id}/investigation-report", UUID.randomUUID())
                        .with(user("investigator").roles("AI_GOVERNANCE_OFFICER")))
                .andExpect(status().isNotFound());
    }

    private CreatedException createUnmatchedException(String reference) throws Exception {
        String paymentBody = """
                {
                  "source": "INTERNAL",
                  "transactionReference": "%s",
                  "endToEndId": "E2E-%s",
                  "amount": 43.20,
                  "currency": "EUR",
                  "counterparty": "AI Test Bank",
                  "valueDate": "%s",
                  "status": "RECEIVED"
                }
                """.formatted(reference, reference, LocalDate.parse("2026-11-18"));
        String paymentResponse = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        PaymentRecordResponse payment = objectMapper.readValue(paymentResponse, PaymentRecordResponse.class);

        String batchResponse = mockMvc.perform(post("/api/v1/reconciliation/batches")
                        .with(user("reconciler").roles("RECONCILIATION_OFFICER")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID batchId = objectMapper.readTree(batchResponse).get("id").traverse(objectMapper)
                .readValueAs(UUID.class);
        String resultsBody = mockMvc.perform(get("/api/v1/reconciliation/batches/{id}/results", batchId)
                        .with(user("reconciler").roles("RECONCILIATION_OFFICER")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<ReconciliationResultResponse> results = objectMapper.readValue(
                resultsBody, new TypeReference<List<ReconciliationResultResponse>>() {});
        UUID resultId = results.stream()
                .filter(result -> result.status() == com.pswied.core.reconciliation.ReconciliationResultStatus.UNMATCHED)
                .filter(result -> payment.id().equals(result.internalRecordId()))
                .map(ReconciliationResultResponse::id)
                .findFirst()
                .orElseThrow();

        String exceptionsBody = mockMvc.perform(get("/api/v1/exceptions")
                        .with(user("investigator").roles("PAYMENT_OPERATOR")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<PaymentExceptionResponse> exceptions = objectMapper.readValue(
                exceptionsBody, new TypeReference<List<PaymentExceptionResponse>>() {});
        UUID exceptionId = exceptions.stream()
                .filter(exception -> resultId.equals(exception.reconciliationResultId()))
                .map(PaymentExceptionResponse::id)
                .findFirst()
                .orElseThrow();
        return new CreatedException(exceptionId, payment.id());
    }

    private record CreatedException(UUID exceptionId, UUID paymentRecordId) {
    }
}
