package com.pswied.core.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pswied.core.ArusAiApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ArusAiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRecordRepository paymentRecordRepository;

    @Autowired
    private PaymentImportIdempotencyRepository idempotencyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void importsJsonIdempotentlyAndReportsDuplicates() throws Exception {
        String request = jsonImport("JSON-IMPORT-1", "JSON-IMPORT-2");

        String firstResponse = mockMvc.perform(post("/api/v1/payments/import")
                        .header("Idempotency-Key", "json-import-request-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotency-Replayed", "false"))
                .andExpect(jsonPath("$.totalRows").value(2))
                .andExpect(jsonPath("$.createdCount").value(2))
                .andExpect(jsonPath("$.duplicateCount").value(0))
                .andExpect(jsonPath("$.results[0].outcome").value("CREATED"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String importId = objectMapper.readTree(firstResponse).get("importId").asText();

        mockMvc.perform(post("/api/v1/payments/import")
                        .header("Idempotency-Key", "json-import-request-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andExpect(jsonPath("$.importId").value(importId));

        mockMvc.perform(post("/api/v1/payments/import")
                        .header("Idempotency-Key", "json-import-request-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonImport("JSON-IMPORT-DIFFERENT")))
                .andExpect(status().isConflict());

        String duplicateReference = jsonImport("JSON-IMPORT-1");
        mockMvc.perform(post("/api/v1/payments/import")
                        .header("Idempotency-Key", "json-import-request-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateReference))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(0))
                .andExpect(jsonPath("$.duplicateCount").value(1))
                .andExpect(jsonPath("$.results[0].outcome").value("DUPLICATE"));

        assertThat(paymentRecordRepository.countBySourceAndTransactionReference(
                PaymentSource.INTERNAL, "JSON-IMPORT-1")).isEqualTo(1);
        assertThat(idempotencyRepository.findById("json-import-request-1")).isPresent();
        assertThat(idempotencyRepository.findById("json-import-request-2")).isPresent();
    }

    @Test
    void reportsRepeatedReferencesWithinOneBatchAsDuplicates() throws Exception {
        mockMvc.perform(post("/api/v1/payments/import")
                        .header("Idempotency-Key", "intra-batch-duplicates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonImport("REPEATED-REFERENCE", "REPEATED-REFERENCE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(1))
                .andExpect(jsonPath("$.duplicateCount").value(1))
                .andExpect(jsonPath("$.results[0].outcome").value("CREATED"))
                .andExpect(jsonPath("$.results[1].outcome").value("DUPLICATE"));

        assertThat(paymentRecordRepository.countBySourceAndTransactionReference(
                PaymentSource.INTERNAL, "REPEATED-REFERENCE")).isEqualTo(1);
    }

    @Test
    void importsCsvAndListsTheImportedRecords() throws Exception {
        String csv = """
                source,transactionReference,endToEndId,amount,currency,counterparty,valueDate,status
                EXTERNAL,CSV-IMPORT-1,E2E-CSV-1,36.25,EUR,CSV Counterparty,2026-11-18,SETTLED
                """;

        mockMvc.perform(post("/api/v1/payments/import")
                        .header("Idempotency-Key", "csv-import-request-1")
                        .contentType("text/csv")
                        .content(csv))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotency-Replayed", "false"))
                .andExpect(jsonPath("$.createdCount").value(1))
                .andExpect(jsonPath("$.results[0].payment.source").value("EXTERNAL"));

        mockMvc.perform(get("/api/v1/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.transactionReference == 'CSV-IMPORT-1')]").exists());
    }

    @Test
    void rejectsInvalidCsvRowsAndHeaders() throws Exception {
        String invalidRow = """
                source,transactionReference,endToEndId,amount,currency,counterparty,valueDate,status
                INTERNAL,BAD-CSV-1,E2E-BAD,0,eur,Counterparty,not-a-date,UNKNOWN
                """;

        mockMvc.perform(post("/api/v1/payments/import")
                        .header("Idempotency-Key", "invalid-csv-1")
                        .contentType("text/csv")
                        .content(invalidRow))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());

        mockMvc.perform(post("/api/v1/payments/import")
                        .header("Idempotency-Key", "invalid-csv-headers")
                        .contentType("text/csv")
                        .content("reference,amount\nA,1.00\n"))
                .andExpect(status().isBadRequest());

        assertThat(paymentRecordRepository.countBySourceAndTransactionReference(
                PaymentSource.INTERNAL, "BAD-CSV-1")).isZero();
    }

    @Test
    void rejectsJsonImportValidationErrorsAndMissingIdempotencyKey() throws Exception {
        mockMvc.perform(post("/api/v1/payments/import")
                        .header("Idempotency-Key", "invalid-json-import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "records": [{
                                    "source": "INTERNAL",
                                    "transactionReference": "INVALID-JSON",
                                    "endToEndId": "E2E-INVALID",
                                    "amount": 12.50,
                                    "currency": "eur",
                                    "counterparty": "JSON Counterparty",
                                    "valueDate": "2026-11-18",
                                    "status": "RECEIVED"
                                  }]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());

        mockMvc.perform(post("/api/v1/payments/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonImport("MISSING-KEY")))
                .andExpect(status().isBadRequest());
    }

    private String jsonImport(String... references) throws Exception {
        var records = java.util.Arrays.stream(references)
                .map(reference -> new PaymentRecordRequest(
                        PaymentSource.INTERNAL,
                        reference,
                        "E2E-" + reference,
                        new java.math.BigDecimal("12.50"),
                        "EUR",
                        "JSON Counterparty",
                        java.time.LocalDate.parse("2026-11-18"),
                        PaymentStatus.RECEIVED))
                .toList();
        return objectMapper.writeValueAsString(new PaymentImportRequest(records));
    }
}
