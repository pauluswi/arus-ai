package com.pswied.core.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PaymentRecordsIngestedListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentRecordsIngestedListener.class);

    @Async("localEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PaymentRecordsIngestedEvent event) {
        LOGGER.info(
                "Payment import {} committed with {} new records",
                event.importId(),
                event.paymentRecordIds().size());
    }
}
