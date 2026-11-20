package com.pswied.core.payment;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, UUID> {

    boolean existsBySourceAndTransactionReference(PaymentSource source, String transactionReference);

    long countBySourceAndTransactionReference(PaymentSource source, String transactionReference);

    List<PaymentRecord> findBySourceAndTransactionReferenceIn(
            PaymentSource source,
            Collection<String> transactionReferences);

    List<PaymentRecord> findBySourceOrderByValueDateAscTransactionReferenceAscIdAsc(PaymentSource source);

    long countByDemoRecordTrue();

    long countByDemoRecordTrueAndSource(PaymentSource source);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PaymentRecord record where record.demoRecord = true")
    int deleteAllDemoRecords();
}
