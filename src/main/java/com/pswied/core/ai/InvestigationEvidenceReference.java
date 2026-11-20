package com.pswied.core.ai;

import java.util.UUID;

public record InvestigationEvidenceReference(
        InvestigationEvidenceType type,
        UUID id,
        String description) {
}
