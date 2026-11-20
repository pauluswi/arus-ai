package com.pswied.core.approval;

import com.pswied.core.exception.ExceptionApprovalDecisionRequest;
import com.pswied.core.exception.ExceptionResolutionRequestResponse;
import com.pswied.core.exception.ExceptionWorkflowService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/approvals")
public class ExceptionApprovalController {

    private final ExceptionWorkflowService workflowService;

    public ExceptionApprovalController(ExceptionWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('OPERATIONS_MANAGER', 'COMPLIANCE_OFFICER')")
    public List<ExceptionResolutionRequestResponse> pending() {
        return workflowService.pendingApprovals();
    }

    @PostMapping("/exception-resolution-requests/{requestId}/decision")
    @PreAuthorize("hasAnyRole('OPERATIONS_MANAGER', 'COMPLIANCE_OFFICER')")
    public ApprovalResponse decide(
            @PathVariable UUID requestId,
            @Valid @RequestBody ExceptionApprovalDecisionRequest request,
            Authentication authentication) {
        return workflowService.decide(
                requestId, request.decision(), request.justification(), authentication.getName());
    }
}
