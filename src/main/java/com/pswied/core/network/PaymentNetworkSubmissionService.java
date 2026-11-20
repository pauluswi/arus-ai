package com.pswied.core.network;

import com.pswied.core.payment.PaymentRecord;
import com.pswied.core.payment.PaymentRecordNotFoundException;
import com.pswied.core.payment.PaymentRecordRepository;
import com.pswied.core.config.DomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentNetworkSubmissionService {

    private final PaymentRecordRepository paymentRecordRepository;
    private final PaymentNetworkSubmissionStore submissionCache;
    private final DomainEventPublisher eventPublisher;

    public PaymentNetworkSubmissionService(
            PaymentRecordRepository paymentRecordRepository,
            PaymentNetworkSubmissionStore submissionCache,
            DomainEventPublisher eventPublisher) {
        this.paymentRecordRepository = paymentRecordRepository;
        this.submissionCache = submissionCache;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public NetworkSubmissionStatusView submit(UUID paymentRecordId) {
        paymentRecordRepository.findById(paymentRecordId)
                .orElseThrow(() -> new PaymentRecordNotFoundException(paymentRecordId));

        NetworkSubmissionStatusView pending = NetworkSubmissionStatusView.pending(
                UUID.randomUUID(),
                paymentRecordId,
                Instant.now());
        submissionCache.put(pending);
        eventPublisher.publish(new PaymentNetworkSubmissionRequested(
                pending.submissionId(),
                paymentRecordId));
        return pending;
    }

    public NetworkSubmissionStatusView get(UUID submissionId) {
        return submissionCache.get(submissionId);
    }

    PaymentRecord findPaymentRecord(UUID id) {
        return paymentRecordRepository.findById(id)
                .orElseThrow(() -> new PaymentRecordNotFoundException(id));
    }
}
