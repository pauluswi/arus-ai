package com.pswied.core.network;

import java.time.Instant;
import java.util.UUID;

public record NetworkSubmissionResult(
        UUID paymentRecordId,
        String networkReference,
        NetworkSubmissionStatus status,
        String responseCode,
        String message,
        Instant respondedAt) {
}
