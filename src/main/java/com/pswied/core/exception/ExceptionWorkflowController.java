package com.pswied.core.exception;

import com.pswied.core.approval.ApprovalResponse;
import com.pswied.core.audit.AuditLogResponse;
import com.pswied.core.investigation.InvestigationNoteResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exceptions")
public class ExceptionWorkflowController {

    private final ExceptionWorkflowService workflowService;

    public ExceptionWorkflowController(ExceptionWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PAYMENT_OPERATOR', 'RECONCILIATION_OFFICER', 'OPERATIONS_MANAGER', 'COMPLIANCE_OFFICER', 'PAYMENT_ADMIN')")
    public List<PaymentExceptionResponse> list() {
        return workflowService.list();
    }

    @GetMapping("/{exceptionId}")
    @PreAuthorize("hasAnyRole('PAYMENT_OPERATOR', 'RECONCILIATION_OFFICER', 'OPERATIONS_MANAGER', 'COMPLIANCE_OFFICER', 'PAYMENT_ADMIN')")
    public PaymentExceptionResponse get(@PathVariable UUID exceptionId) {
        return workflowService.get(exceptionId);
    }

    @PutMapping("/{exceptionId}/assignment")
    @PreAuthorize("hasAnyRole('OPERATIONS_MANAGER', 'PAYMENT_ADMIN', 'RECONCILIATION_OFFICER')")
    public PaymentExceptionResponse assign(
            @PathVariable UUID exceptionId,
            @Valid @RequestBody ExceptionAssignmentRequest request,
            Authentication authentication) {
        return workflowService.assign(exceptionId, request.assignedTo(), authentication.getName());
    }

    @PutMapping("/{exceptionId}/severity")
    @PreAuthorize("hasAnyRole('OPERATIONS_MANAGER', 'COMPLIANCE_OFFICER', 'PAYMENT_ADMIN')")
    public PaymentExceptionResponse updateSeverity(
            @PathVariable UUID exceptionId,
            @Valid @RequestBody ExceptionSeverityRequest request,
            Authentication authentication) {
        return workflowService.updateSeverity(exceptionId, request.severity(), authentication.getName());
    }

    @PostMapping("/{exceptionId}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PAYMENT_OPERATOR', 'RECONCILIATION_OFFICER', 'OPERATIONS_MANAGER')")
    public InvestigationNoteResponse addNote(
            @PathVariable UUID exceptionId,
            @Valid @RequestBody InvestigationNoteRequest request,
            Authentication authentication) {
        return workflowService.addNote(exceptionId, request.note(), authentication.getName());
    }

    @GetMapping("/{exceptionId}/notes")
    @PreAuthorize("hasAnyRole('PAYMENT_OPERATOR', 'RECONCILIATION_OFFICER', 'OPERATIONS_MANAGER', 'COMPLIANCE_OFFICER', 'PAYMENT_ADMIN')")
    public List<InvestigationNoteResponse> notes(@PathVariable UUID exceptionId) {
        return workflowService.notes(exceptionId);
    }

    @PostMapping("/{exceptionId}/resolution-requests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PAYMENT_OPERATOR', 'RECONCILIATION_OFFICER', 'OPERATIONS_MANAGER')")
    public ExceptionResolutionRequestResponse requestResolution(
            @PathVariable UUID exceptionId,
            @Valid @RequestBody ExceptionResolutionRequestBody request,
            Authentication authentication) {
        return workflowService.requestResolution(
                exceptionId, request.action(), request.justification(), authentication.getName());
    }

    @GetMapping("/{exceptionId}/approvals")
    @PreAuthorize("hasAnyRole('OPERATIONS_MANAGER', 'COMPLIANCE_OFFICER', 'PAYMENT_ADMIN')")
    public List<ApprovalResponse> approvals(@PathVariable UUID exceptionId) {
        return workflowService.approvals(exceptionId);
    }

    @GetMapping("/{exceptionId}/audit")
    @PreAuthorize("hasAnyRole('RECONCILIATION_OFFICER', 'OPERATIONS_MANAGER', 'COMPLIANCE_OFFICER', 'PAYMENT_ADMIN')")
    public List<AuditLogResponse> auditHistory(@PathVariable UUID exceptionId) {
        return workflowService.auditHistory(exceptionId);
    }
}
