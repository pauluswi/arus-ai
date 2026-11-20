package com.pswied.core.ai;

import com.pswied.core.exception.PaymentExceptionCategory;

import java.util.List;
import java.util.UUID;

public record InvestigationReport(
        UUID exceptionId,
        PaymentExceptionCategory category,
        String provider,
        String fixtureId,
        String summary,
        ConfidenceIndicator confidence,
        List<InvestigationRecommendation> recommendations,
        List<InvestigationEvidenceReference> evidenceReferences,
        String disclaimer) {
}
