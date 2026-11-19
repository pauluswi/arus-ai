package com.pswied.core.network;

import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PaymentNetworkSubmissionCache {

    private final Cache<UUID, NetworkSubmissionStatusView> cache;

    public PaymentNetworkSubmissionCache(Cache<UUID, NetworkSubmissionStatusView> cache) {
        this.cache = cache;
    }

    public void put(NetworkSubmissionStatusView status) {
        cache.put(status.submissionId(), status);
    }

    public NetworkSubmissionStatusView get(UUID submissionId) {
        NetworkSubmissionStatusView status = cache.getIfPresent(submissionId);
        if (status == null) {
            throw new NetworkSubmissionNotFoundException(submissionId);
        }
        return status;
    }
}
