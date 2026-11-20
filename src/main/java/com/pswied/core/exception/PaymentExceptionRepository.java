package com.pswied.core.exception;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentExceptionRepository extends JpaRepository<PaymentException, UUID> {

    List<PaymentException> findAllByOrderByCreatedAtDesc();

    List<PaymentException> findByReconciliationResultIdIn(Iterable<UUID> reconciliationResultIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select paymentException from PaymentException paymentException where paymentException.id = :id")
    Optional<PaymentException> findByIdForUpdate(@Param("id") UUID id);
}
