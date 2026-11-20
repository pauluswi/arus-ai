package com.pswied.core.network;

import com.pswied.core.payment.PaymentRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
public class PaymentNetworkSubmissionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentNetworkSubmissionListener.class);

    private final PaymentNetworkSubmissionService submissionService;
    private final PaymentNetworkAdapter paymentNetworkAdapter;
    private final PaymentNetworkSubmissionStore submissionCache;

    public PaymentNetworkSubmissionListener(
            PaymentNetworkSubmissionService submissionService,
            PaymentNetworkAdapter paymentNetworkAdapter,
            PaymentNetworkSubmissionStore submissionCache) {
        this.submissionService = submissionService;
        this.paymentNetworkAdapter = paymentNetworkAdapter;
        this.submissionCache = submissionCache;
    }

    @Async("localEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PaymentNetworkSubmissionRequested event) {
        NetworkSubmissionStatusView pending = submissionCache.get(event.submissionId());
        try {
            PaymentRecord paymentRecord = submissionService.findPaymentRecord(event.paymentRecordId());
            NetworkSubmissionResult result = paymentNetworkAdapter.submit(paymentRecord);
            submissionCache.put(NetworkSubmissionStatusView.completed(pending, result));
        } catch (RuntimeException exception) {
            LOGGER.error("Payment network simulation failed for submission {}", event.submissionId(), exception);
            submissionCache.put(NetworkSubmissionStatusView.failed(
                    pending,
                    "The local payment network simulator could not process the request."));
        }
    }
}
