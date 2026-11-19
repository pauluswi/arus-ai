package com.pswied.core.network;

import java.time.Instant;
import java.util.UUID;

public record NetworkSubmissionStatusView(
        UUID submissionId,
        UUID paymentRecordId,
        NetworkSubmissionStatus status,
        NetworkSubmissionResult result,
        String failureReason,
        Instant submittedAt,
        Instant completedAt) {

    public static NetworkSubmissionStatusView pending(UUID submissionId, UUID paymentRecordId, Instant submittedAt) {
        return new NetworkSubmissionStatusView(
                submissionId,
                paymentRecordId,
                NetworkSubmissionStatus.PENDING,
                null,
                null,
                submittedAt,
                null);
    }

    public static NetworkSubmissionStatusView completed(
            NetworkSubmissionStatusView pending,
            NetworkSubmissionResult result) {
        return new NetworkSubmissionStatusView(
                pending.submissionId(),
                pending.paymentRecordId(),
                result.status(),
                result,
                null,
                pending.submittedAt(),
                result.respondedAt());
    }

    public static NetworkSubmissionStatusView failed(NetworkSubmissionStatusView pending, String reason) {
        return new NetworkSubmissionStatusView(
                pending.submissionId(),
                pending.paymentRecordId(),
                NetworkSubmissionStatus.FAILED,
                null,
                reason,
                pending.submittedAt(),
                Instant.now());
    }
}
