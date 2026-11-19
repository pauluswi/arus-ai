package com.pswied.core.network;

import com.pswied.core.payment.PaymentRecord;
import com.pswied.core.payment.PaymentStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Profile({"local", "test"})
public class LocalPaymentNetworkSimulator implements PaymentNetworkAdapter {

    @Override
    public NetworkSubmissionResult submit(PaymentRecord paymentRecord) {
        boolean rejected = paymentRecord.getStatus() == PaymentStatus.FAILED
                || paymentRecord.getStatus() == PaymentStatus.REJECTED;
        String networkReference = "SIM-%s-%s".formatted(
                paymentRecord.getSource(),
                paymentRecord.getTransactionReference());

        if (rejected) {
            return new NetworkSubmissionResult(
                    paymentRecord.getId(),
                    networkReference,
                    NetworkSubmissionStatus.REJECTED,
                    "RJCT",
                    "The simulated network rejected a payment marked failed or rejected.",
                    Instant.now());
        }

        return new NetworkSubmissionResult(
                paymentRecord.getId(),
                networkReference,
                NetworkSubmissionStatus.SUCCEEDED,
                "ACCP",
                "The simulated network accepted the payment.",
                Instant.now());
    }
}
