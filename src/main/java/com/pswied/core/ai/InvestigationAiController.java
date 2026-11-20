package com.pswied.core.ai;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exceptions/{exceptionId}/investigation-report")
public class InvestigationAiController {

    private final InvestigationAiService investigationAiService;

    public InvestigationAiController(InvestigationAiService investigationAiService) {
        this.investigationAiService = investigationAiService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PAYMENT_OPERATOR', 'RECONCILIATION_OFFICER', 'OPERATIONS_MANAGER', "
            + "'COMPLIANCE_OFFICER', 'PAYMENT_ADMIN', 'AI_GOVERNANCE_OFFICER')")
    public InvestigationReport report(@PathVariable UUID exceptionId) {
        return investigationAiService.generateReport(exceptionId);
    }
}
