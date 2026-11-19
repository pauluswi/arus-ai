package com.pswied.core.demo;

import com.pswied.core.payment.PaymentRecord;
import com.pswied.core.payment.PaymentRecordRepository;
import com.pswied.core.payment.PaymentSource;
import com.pswied.core.payment.PaymentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class DemoDataService {

    private static final Instant CREATED_AT = Instant.parse("2026-11-18T09:00:00Z");
    private static final LocalDate VALUE_DATE = LocalDate.parse("2026-11-18");

    private final PaymentRecordRepository paymentRecordRepository;

    public DemoDataService(PaymentRecordRepository paymentRecordRepository) {
        this.paymentRecordRepository = paymentRecordRepository;
    }

    @Transactional
    public SeedResult initialize(boolean reset) {
        if (reset) {
            paymentRecordRepository.deleteAllDemoRecords();
        }

        if (paymentRecordRepository.countByDemoRecordTrue() == 0) {
            paymentRecordRepository.saveAll(demoRecords());
        }

        return new SeedResult(
                paymentRecordRepository.countByDemoRecordTrue(),
                paymentRecordRepository.countByDemoRecordTrueAndSource(PaymentSource.INTERNAL),
                paymentRecordRepository.countByDemoRecordTrueAndSource(PaymentSource.EXTERNAL));
    }

    private List<PaymentRecord> demoRecords() {
        return List.of(
                record(PaymentSource.INTERNAL, "INT-1001", "E2E-1001", "1250.00", "EUR", "Northwind GmbH", PaymentStatus.SETTLED),
                record(PaymentSource.INTERNAL, "INT-1002", "E2E-1002", "87.45", "EUR", "Contoso BV", PaymentStatus.SETTLED),
                record(PaymentSource.INTERNAL, "INT-1003", "E2E-1003", "250.00", "EUR", "Fabrikam SA", PaymentStatus.PENDING),
                record(PaymentSource.INTERNAL, "INT-1004", "E2E-1004", "500.00", "EUR", "Adventure Works", PaymentStatus.SETTLED),
                record(PaymentSource.INTERNAL, "INT-1005", "E2E-1005", "19.99", "EUR", "Tailspin Toys", PaymentStatus.FAILED),
                record(PaymentSource.INTERNAL, "INT-1006", "E2E-1006", "640.00", "EUR", "Woodgrove Bank", PaymentStatus.SETTLED),
                record(PaymentSource.EXTERNAL, "EXT-9001", "E2E-1001", "1250.00", "EUR", "Northwind GmbH", PaymentStatus.SETTLED),
                record(PaymentSource.EXTERNAL, "EXT-9002", "E2E-1002", "87.45", "EUR", "Contoso BV", PaymentStatus.SETTLED),
                record(PaymentSource.EXTERNAL, "EXT-9004", "E2E-1004", "495.00", "EUR", "Adventure Works", PaymentStatus.SETTLED),
                record(PaymentSource.EXTERNAL, "EXT-9005", "E2E-1005", "19.99", "EUR", "Tailspin Toys", PaymentStatus.REJECTED),
                record(PaymentSource.EXTERNAL, "EXT-9007", "E2E-1007", "300.00", "EUR", "Litware Inc", PaymentStatus.SETTLED));
    }

    private PaymentRecord record(
            PaymentSource source,
            String reference,
            String endToEndId,
            String amount,
            String currency,
            String counterparty,
            PaymentStatus status) {
        return new PaymentRecord(
                UUID.nameUUIDFromBytes((source + ":" + reference).getBytes(StandardCharsets.UTF_8)),
                source,
                reference,
                endToEndId,
                new BigDecimal(amount),
                currency,
                counterparty,
                VALUE_DATE,
                status,
                CREATED_AT,
                true);
    }

    public record SeedResult(long demoRecords, long internalRecords, long externalRecords) {
    }
}
