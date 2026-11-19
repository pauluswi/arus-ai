package com.pswied.core.network;

import com.pswied.core.payment.PaymentRecord;

public interface PaymentNetworkAdapter {

    NetworkSubmissionResult submit(PaymentRecord paymentRecord);
}
