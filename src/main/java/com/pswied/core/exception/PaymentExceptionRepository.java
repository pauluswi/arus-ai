package com.pswied.core.exception;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentExceptionRepository extends JpaRepository<PaymentException, UUID> {
}
