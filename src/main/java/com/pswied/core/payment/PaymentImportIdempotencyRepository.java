package com.pswied.core.payment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentImportIdempotencyRepository extends JpaRepository<PaymentImportIdempotencyRecord, String> {
}
