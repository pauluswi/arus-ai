package com.pswied.core.ai;

import java.util.List;

public record InvestigationRecommendation(
        String action,
        String rationale,
        ConfidenceIndicator confidence,
        List<String> evidenceReferences) {
}
