package com.pswied.core.exception;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pswied.core.ArusAiApplication;
import com.pswied.core.audit.AuditLogResponse;
import com.pswied.core.approval.ApprovalResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ArusAiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExceptionWorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void managesExceptionWithAssignmentInvestigationAndSeparateApproval() throws Exception {
        UUID exceptionId = createUnmatchedException("workflow-" + UUID.randomUUID());

        mockMvc.perform(put("/api/v1/exceptions/{id}/assignment", exceptionId)
                        .with(user("operator").roles("PAYMENT_OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assignedTo":"case-worker"}
                                """))
                .andExpect(status().isForbidden());

        String assigned = mockMvc.perform(put("/api/v1/exceptions/{id}/assignment", exceptionId)
                        .with(user("manager").roles("OPERATIONS_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assignedTo":"case-worker"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(objectMapper.readValue(assigned, PaymentExceptionResponse.class).assignedTo())
                .isEqualTo("case-worker");

        mockMvc.perform(put("/api/v1/exceptions/{id}/severity", exceptionId)
                        .with(user("manager").roles("OPERATIONS_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"severity":"HIGH"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value(2));

        String noteBody = mockMvc.perform(post("/api/v1/exceptions/{id}/notes", exceptionId)
                        .with(user("case-worker").roles("PAYMENT_OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"note":"Compared the source payment and confirmed no network counterpart exists."}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        InvestigationNoteResponse note = objectMapper.readValue(noteBody, InvestigationNoteResponse.class);
        assertThat(note.author()).isEqualTo("case-worker");

        String resolutionBody = mockMvc.perform(post(
                                "/api/v1/exceptions/{id}/resolution-requests", exceptionId)
                        .with(user("case-worker").roles("PAYMENT_OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"RESOLVE","justification":"The external institution confirmed no settlement occurred."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        ExceptionResolutionRequestResponse request =
                objectMapper.readValue(resolutionBody, ExceptionResolutionRequestResponse.class);

        mockMvc.perform(get("/api/v1/approvals/pending")
                        .with(user("manager").roles("OPERATIONS_MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '%s')]".formatted(request.id())).exists());
        mockMvc.perform(post("/api/v1/approvals/exception-resolution-requests/{id}/decision", request.id())
                        .with(user("case-worker").roles("OPERATIONS_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision":"APPROVED","justification":"Attempted self approval."}
                                """))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/approvals/exception-resolution-requests/{id}/decision", request.id())
                        .with(user("operator").roles("PAYMENT_OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision":"APPROVED","justification":"Not an approver."}
                                """))
                .andExpect(status().isForbidden());

        String approvalBody = mockMvc.perform(post(
                                "/api/v1/approvals/exception-resolution-requests/{id}/decision", request.id())
                        .with(user("independent-manager").roles("OPERATIONS_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision":"APPROVED","justification":"Evidence supports resolving this exception."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("APPROVED"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        ApprovalResponse approval = objectMapper.readValue(approvalBody, ApprovalResponse.class);
        assertThat(approval.decidedBy()).isEqualTo("independent-manager");

        mockMvc.perform(get("/api/v1/exceptions/{id}", exceptionId)
                        .with(user("operator").roles("PAYMENT_OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
        String approvals = mockMvc.perform(get("/api/v1/exceptions/{id}/approvals", exceptionId)
                        .with(user("manager").roles("OPERATIONS_MANAGER")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(objectMapper.readValue(approvals, new TypeReference<List<ApprovalResponse>>() {}))
                .hasSize(1);

        String audit = mockMvc.perform(get("/api/v1/exceptions/{id}/audit", exceptionId)
                        .with(user("manager").roles("OPERATIONS_MANAGER")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<AuditLogResponse> auditHistory =
                objectMapper.readValue(audit, new TypeReference<List<AuditLogResponse>>() {});
        assertThat(auditHistory)
                .extracting(AuditLogResponse::action)
                .contains("ASSIGNED", "SEVERITY_UPDATED", "INVESTIGATION_NOTE_ADDED",
                        "RESOLUTION_REQUESTED", "RESOLUTION_APPROVED");

        String closeRequest = mockMvc.perform(post(
                                "/api/v1/exceptions/{id}/resolution-requests", exceptionId)
                        .with(user("case-worker").roles("PAYMENT_OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"CLOSE","justification":"All operational follow-up is complete."}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        ExceptionResolutionRequestResponse close =
                objectMapper.readValue(closeRequest, ExceptionResolutionRequestResponse.class);
        mockMvc.perform(post("/api/v1/approvals/exception-resolution-requests/{id}/decision", close.id())
                        .with(user("independent-manager").roles("COMPLIANCE_OFFICER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision":"APPROVED","justification":"Close approved after review."}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/exceptions/{id}", exceptionId)
                        .with(user("operator").roles("PAYMENT_OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void rejectedApprovalReturnsExceptionToInvestigation() throws Exception {
        UUID exceptionId = createUnmatchedException("rejection-" + UUID.randomUUID());
        addNote(exceptionId, "worker");
        String body = mockMvc.perform(post("/api/v1/exceptions/{id}/resolution-requests", exceptionId)
                        .with(user("worker").roles("PAYMENT_OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"RESOLVE","justification":"Requesting resolution."}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        ExceptionResolutionRequestResponse request =
                objectMapper.readValue(body, ExceptionResolutionRequestResponse.class);

        mockMvc.perform(post("/api/v1/approvals/exception-resolution-requests/{id}/decision", request.id())
                        .with(user("reviewer").roles("COMPLIANCE_OFFICER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision":"REJECTED","justification":"The institution confirmation is missing."}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/exceptions/{id}", exceptionId)
                        .with(user("operator").roles("PAYMENT_OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INVESTIGATING"));
    }

    private UUID createUnmatchedException(String reference) throws Exception {
        String paymentBody = """
                {
                  "source": "INTERNAL",
                  "transactionReference": "%s",
                  "endToEndId": "E2E-%s",
                  "amount": 43.20,
                  "currency": "EUR",
                  "counterparty": "Workflow Test Bank",
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
        ReconciliationResultResponse result = results.stream()
                .filter(row -> payment.id().equals(row.internalRecordId()))
                .findFirst()
                .orElseThrow();

        String exceptionsBody = mockMvc.perform(get("/api/v1/exceptions")
                        .with(user("operator").roles("PAYMENT_OPERATOR")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<PaymentExceptionResponse> exceptions = objectMapper.readValue(
                exceptionsBody, new TypeReference<List<PaymentExceptionResponse>>() {});
        UUID exceptionId = exceptions.stream()
                .filter(exception -> result.id().equals(exception.reconciliationResultId()))
                .map(PaymentExceptionResponse::id)
                .findFirst()
                .orElseThrow();

        mockMvc.perform(post("/api/v1/reconciliation/batches")
                        .with(user("reconciler").roles("RECONCILIATION_OFFICER")))
                .andExpect(status().isCreated());
        String repeatedExceptionsBody = mockMvc.perform(get("/api/v1/exceptions")
                        .with(user("operator").roles("PAYMENT_OPERATOR")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<PaymentExceptionResponse> repeatedExceptions = objectMapper.readValue(
                repeatedExceptionsBody, new TypeReference<List<PaymentExceptionResponse>>() {});
        assertThat(repeatedExceptions).hasSameSizeAs(exceptions);
        return exceptionId;
    }

    private void addNote(UUID exceptionId, String author) throws Exception {
        mockMvc.perform(post("/api/v1/exceptions/{id}/notes", exceptionId)
                        .with(SecurityMockMvcRequestPostProcessors.user(author).roles("PAYMENT_OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"note":"Investigation evidence recorded."}
                                """))
                .andExpect(status().isCreated());
    }
}
