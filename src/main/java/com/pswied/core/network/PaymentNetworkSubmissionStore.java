package com.pswied.core.network;

import java.util.UUID;

public interface PaymentNetworkSubmissionStore {

    void put(NetworkSubmissionStatusView status);

    NetworkSubmissionStatusView get(UUID submissionId);
}
