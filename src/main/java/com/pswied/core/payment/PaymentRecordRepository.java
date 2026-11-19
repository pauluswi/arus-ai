package com.pswied.core.payment;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, UUID> {

    long countByDemoRecordTrue();

    long countByDemoRecordTrueAndSource(PaymentSource source);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PaymentRecord record where record.demoRecord = true")
    int deleteAllDemoRecords();
}
