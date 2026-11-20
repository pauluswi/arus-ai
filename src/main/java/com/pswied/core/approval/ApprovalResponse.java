package com.pswied.core.approval;

import java.time.Instant;
import java.util.UUID;

public record ApprovalResponse(
        UUID id,
        ApprovalTargetType targetType,
        UUID targetId,
        ApprovalDecision decision,
        String decidedBy,
        String justification,
        Instant decidedAt) {

    public static ApprovalResponse from(Approval approval) {
        return new ApprovalResponse(
                approval.getId(),
                approval.getTargetType(),
                approval.getTargetId(),
                approval.getDecision(),
                approval.getDecidedBy(),
                approval.getJustification(),
                approval.getDecidedAt());
    }
}
