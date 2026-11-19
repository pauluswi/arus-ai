package com.pswied.core.network;

import com.pswied.core.payment.PaymentRecord;
import com.pswied.core.payment.PaymentRecordNotFoundException;
import com.pswied.core.payment.PaymentRecordRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentNetworkSubmissionService {

    private final PaymentRecordRepository paymentRecordRepository;
    private final PaymentNetworkSubmissionCache submissionCache;
    private final ApplicationEventPublisher eventPublisher;

    public PaymentNetworkSubmissionService(
            PaymentRecordRepository paymentRecordRepository,
            PaymentNetworkSubmissionCache submissionCache,
            ApplicationEventPublisher eventPublisher) {
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
        eventPublisher.publishEvent(new PaymentNetworkSubmissionRequested(
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
