package com.pswied.core.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/v1/payments/import")
public class PaymentImportController {

    private final PaymentImportService paymentImportService;
    private final PaymentCsvParser csvParser;

    public PaymentImportController(PaymentImportService paymentImportService, PaymentCsvParser csvParser) {
        this.paymentImportService = paymentImportService;
        this.csvParser = csvParser;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaymentImportResponse> importJson(
            @RequestHeader("Idempotency-Key") @NotEmpty @Size(max = 120) String idempotencyKey,
            @Valid @RequestBody PaymentImportRequest request) {
        return response(paymentImportService.importRecords(idempotencyKey, request.records()));
    }

    @PostMapping(consumes = "text/csv")
    public ResponseEntity<PaymentImportResponse> importCsv(
            @RequestHeader("Idempotency-Key") @NotEmpty @Size(max = 120) String idempotencyKey,
            @RequestBody String content) {
        List<PaymentRecordRequest> requests = csvParser.parse(content);
        return response(paymentImportService.importRecords(idempotencyKey, requests));
    }

    private ResponseEntity<PaymentImportResponse> response(PaymentImportResult result) {
        return ResponseEntity.ok()
                .header("Idempotency-Replayed", Boolean.toString(result.replayed()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(result.response());
    }
}
