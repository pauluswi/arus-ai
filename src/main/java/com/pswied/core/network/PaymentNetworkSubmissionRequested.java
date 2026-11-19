package com.pswied.core.network;

import java.util.UUID;

public record PaymentNetworkSubmissionRequested(UUID submissionId, UUID paymentRecordId) {
}
