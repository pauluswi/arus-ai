package com.pswied.core.reconciliation;

import com.pswied.core.ArusAiApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = ArusAiApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:arus-ai-reconciliation-scale;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "arus.demo-data.enabled=false"
        })
@ActiveProfiles("test")
class ReconciliationScaleIT {

    private static final int TOTAL_RECORDS = 100_000;
    private static final int RECORDS_PER_SOURCE = TOTAL_RECORDS / 2;
    private static final int INSERT_CHUNK_SIZE = 1_000;
    private static final String INSERT_PAYMENT = """
            INSERT INTO payment_record (
                id, source, transaction_reference, end_to_end_id, amount, currency,
                counterparty, value_date, status, created_at, is_demo
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReconciliationService reconciliationService;

    @Autowired
    private ReconciliationResultRepository resultRepository;

    @Test
    void reconcilesOneHundredThousandPersistedPaymentRecords() {
        insertSource("INTERNAL");
        insertSource("EXTERNAL");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payment_record",
                Long.class)).isEqualTo(TOTAL_RECORDS);

        long start = System.nanoTime();
        ReconciliationBatchResponse response = reconciliationService.run("scale-test");
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(response.status()).isEqualTo(ReconciliationBatchStatus.COMPLETED);
        assertThat(response.resultCount()).isEqualTo(RECORDS_PER_SOURCE);
        assertThat(response.matchedCount()).isEqualTo(RECORDS_PER_SOURCE);
        assertThat(response.discrepancyCount()).isZero();
        assertThat(response.unmatchedCount()).isZero();
        assertThat(resultRepository.countByBatchId(response.id())).isEqualTo(RECORDS_PER_SOURCE);
        System.out.printf(
                "Reconciled %,d persisted records into %,d matched pairs in %,d ms.%n",
                TOTAL_RECORDS, response.resultCount(), elapsedMillis);
    }

    private void insertSource(String source) {
        for (int offset = 0; offset < RECORDS_PER_SOURCE; offset += INSERT_CHUNK_SIZE) {
            int batchOffset = offset;
            int batchSize = Math.min(INSERT_CHUNK_SIZE, RECORDS_PER_SOURCE - offset);
            jdbcTemplate.batchUpdate(INSERT_PAYMENT, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement statement, int row) throws SQLException {
                    int referenceNumber = batchOffset + row;
                    String reference = "SCALE-" + referenceNumber;
                    statement.setObject(1, UUID.randomUUID());
                    statement.setString(2, source);
                    statement.setString(3, reference);
                    statement.setString(4, "E2E-" + referenceNumber);
                    statement.setBigDecimal(5, new BigDecimal("19.95"));
                    statement.setString(6, "EUR");
                    statement.setString(7, "Scale Test Counterparty");
                    statement.setObject(8, LocalDate.parse("2026-11-18"));
                    statement.setString(9, "SETTLED");
                    statement.setTimestamp(10, Timestamp.from(Instant.parse("2026-11-18T09:00:00Z")));
                    statement.setBoolean(11, false);
                }

                @Override
                public int getBatchSize() {
                    return batchSize;
                }
            });
        }
    }
}
