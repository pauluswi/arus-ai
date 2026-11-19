package com.pswied.core.demo;

import com.pswied.core.ArusAiApplication;
import com.pswied.core.payment.PaymentRecord;
import com.pswied.core.payment.PaymentRecordRepository;
import com.pswied.core.payment.PaymentSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = ArusAiApplication.class,
        properties = "spring.datasource.url=jdbc:h2:mem:arus-ai-demo-test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@ActiveProfiles("local")
@Transactional
class DemoDataServiceTest {

    @Autowired
    private DemoDataService demoDataService;

    @Autowired
    private PaymentRecordRepository paymentRecordRepository;

    @Test
    void seedsRepeatableInternalAndExternalRecordsAndSupportsReset() {
        assertThat(paymentRecordRepository.countByDemoRecordTrue()).isEqualTo(11);

        DemoDataService.SeedResult firstSeed = demoDataService.initialize(false);

        assertThat(firstSeed.demoRecords()).isEqualTo(11);
        assertThat(firstSeed.internalRecords()).isEqualTo(6);
        assertThat(firstSeed.externalRecords()).isEqualTo(5);
        assertThat(paymentRecordRepository.countByDemoRecordTrueAndSource(PaymentSource.INTERNAL)).isEqualTo(6);
        assertThat(paymentRecordRepository.countByDemoRecordTrueAndSource(PaymentSource.EXTERNAL)).isEqualTo(5);

        DemoDataService.SeedResult repeatSeed = demoDataService.initialize(false);
        assertThat(repeatSeed.demoRecords()).isEqualTo(11);

        paymentRecordRepository.saveAndFlush(new PaymentRecord(
                UUID.randomUUID(),
                PaymentSource.INTERNAL,
                "REAL-RECORD-1",
                "E2E-REAL-1",
                new BigDecimal("42.00"),
                "EUR",
                "Local test",
                LocalDate.parse("2026-11-18"),
                "SETTLED",
                Instant.parse("2026-11-18T09:00:00Z"),
                false));
        DemoDataService.SeedResult resetSeed = demoDataService.initialize(true);
        assertThat(resetSeed.demoRecords()).isEqualTo(11);
        assertThat(paymentRecordRepository.countByDemoRecordTrue()).isEqualTo(11);
        assertThat(paymentRecordRepository.count()).isEqualTo(12);
    }
}
