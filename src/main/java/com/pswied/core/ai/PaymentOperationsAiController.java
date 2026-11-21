package com.pswied.core.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
@Validated
public class PaymentOperationsAiController {

    private final PaymentOperationsAiService aiService;

    public PaymentOperationsAiController(PaymentOperationsAiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/payments/{paymentRecordId}/match-ranking")
    @PreAuthorize("hasAnyRole('PAYMENT_OPERATOR', 'RECONCILIATION_OFFICER', 'OPERATIONS_MANAGER', "
            + "'COMPLIANCE_OFFICER', 'PAYMENT_ADMIN', 'AI_GOVERNANCE_OFFICER')")
    public List<PaymentOperationsAiService.MatchCandidate> rankCandidates(@PathVariable UUID paymentRecordId) {
        return aiService.rankCandidates(paymentRecordId);
    }

    @GetMapping("/payments/{paymentRecordId}/anomalies")
    @PreAuthorize("hasAnyRole('PAYMENT_OPERATOR', 'RECONCILIATION_OFFICER', 'OPERATIONS_MANAGER', "
            + "'COMPLIANCE_OFFICER', 'PAYMENT_ADMIN', 'AI_GOVERNANCE_OFFICER')")
    public PaymentOperationsAiService.AnomalyReport detectAnomalies(@PathVariable UUID paymentRecordId) {
        return aiService.detectAnomalies(paymentRecordId);
    }

    @GetMapping("/payments/{paymentRecordId}/failure-prediction")
    @PreAuthorize("hasAnyRole('PAYMENT_OPERATOR', 'RECONCILIATION_OFFICER', 'OPERATIONS_MANAGER', "
            + "'COMPLIANCE_OFFICER', 'PAYMENT_ADMIN', 'AI_GOVERNANCE_OFFICER')")
    public PaymentOperationsAiService.FailurePrediction predictFailure(@PathVariable UUID paymentRecordId) {
        return aiService.predictFailure(paymentRecordId);
    }

    @GetMapping("/reconciliation-results/{reconciliationResultId}/classification")
    @PreAuthorize("hasAnyRole('PAYMENT_OPERATOR', 'RECONCILIATION_OFFICER', 'OPERATIONS_MANAGER', "
            + "'COMPLIANCE_OFFICER', 'PAYMENT_ADMIN', 'AI_GOVERNANCE_OFFICER')")
    public PaymentOperationsAiService.ExceptionClassification classifyException(
            @PathVariable UUID reconciliationResultId) {
        return aiService.classifyException(reconciliationResultId);
    }

    @GetMapping("/knowledge")
    @PreAuthorize("hasAnyRole('PAYMENT_OPERATOR', 'RECONCILIATION_OFFICER', 'OPERATIONS_MANAGER', "
            + "'COMPLIANCE_OFFICER', 'PAYMENT_ADMIN', 'AI_GOVERNANCE_OFFICER')")
    public PaymentOperationsAiService.KnowledgeSearchResult searchKnowledge(
            @RequestParam @NotBlank @Size(max = 200) String query) {
        return aiService.searchKnowledge(query);
    }
}
