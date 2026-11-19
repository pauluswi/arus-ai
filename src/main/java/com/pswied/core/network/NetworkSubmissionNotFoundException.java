package com.pswied.core.network;

import java.util.UUID;

public class NetworkSubmissionNotFoundException extends RuntimeException {

    public NetworkSubmissionNotFoundException(UUID submissionId) {
        super("Network submission not found or expired: " + submissionId);
    }
}
