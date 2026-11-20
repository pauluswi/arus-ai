package com.pswied.core.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pswied.core.config.DomainEventPublisher;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class PaymentImportService {

    public static final int MAX_RECORDS = 10_000;

    private final PaymentRecordRepository paymentRecordRepository;
    private final PaymentImportIdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final DomainEventPublisher eventPublisher;

    public PaymentImportService(
            PaymentRecordRepository paymentRecordRepository,
            PaymentImportIdempotencyRepository idempotencyRepository,
            ObjectMapper objectMapper,
            Validator validator,
            DomainEventPublisher eventPublisher) {
        this.paymentRecordRepository = paymentRecordRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public PaymentImportResult importRecords(String idempotencyKey, List<PaymentRecordRequest> requests) {
        validateImport(idempotencyKey, requests);
        String requestHash = hash(requests);

        PaymentImportIdempotencyRecord previous = idempotencyRepository.findById(idempotencyKey).orElse(null);
        if (previous != null) {
            if (!MessageDigest.isEqual(
                    previous.getRequestHash().getBytes(StandardCharsets.US_ASCII),
                    requestHash.getBytes(StandardCharsets.US_ASCII))) {
                throw new PaymentImportIdempotencyConflictException(idempotencyKey);
            }
            return new PaymentImportResult(readResponse(previous.getResponsePayload()), true);
        }

        UUID importId = UUID.randomUUID();
        Map<PaymentKey, PaymentRecord> existingByKey = findExistingRecords(requests);
        Set<PaymentKey> seenKeys = new HashSet<>();
        List<PaymentRecord> newRecords = new ArrayList<>();
        List<PaymentImportItemResult> results = new ArrayList<>(requests.size());
        Map<PaymentKey, PaymentRecord> recordsToReturn = new HashMap<>(existingByKey);
        Instant createdAt = Instant.now();

        for (int index = 0; index < requests.size(); index++) {
            PaymentRecordRequest request = requests.get(index);
            PaymentKey key = PaymentKey.from(request);
            PaymentRecord existing = recordsToReturn.get(key);
            if (existing != null || !seenKeys.add(key)) {
                results.add(new PaymentImportItemResult(
                        index + 1,
                        PaymentImportOutcome.DUPLICATE,
                        PaymentRecordResponse.from(existing),
                        "A record with this source and transaction reference already exists."));
                continue;
            }

            PaymentRecord record = toEntity(request, createdAt);
            newRecords.add(record);
            recordsToReturn.put(key, record);
            results.add(new PaymentImportItemResult(
                    index + 1,
                    PaymentImportOutcome.CREATED,
                    PaymentRecordResponse.from(record),
                    null));
        }

        List<PaymentRecord> savedRecords = paymentRecordRepository.saveAll(newRecords);
        PaymentImportResponse response = new PaymentImportResponse(
                importId,
                requests.size(),
                savedRecords.size(),
                requests.size() - savedRecords.size(),
                results);
        idempotencyRepository.save(new PaymentImportIdempotencyRecord(
                idempotencyKey,
                requestHash,
                writeResponse(response),
                Instant.now()));
        if (!savedRecords.isEmpty()) {
            eventPublisher.publish(new PaymentRecordsIngestedEvent(
                    importId,
                    savedRecords.stream().map(PaymentRecord::getId).toList(),
                    Instant.now()));
        }
        return new PaymentImportResult(response, false);
    }

    private void validateImport(String idempotencyKey, List<PaymentRecordRequest> requests) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 120) {
            throw new PaymentImportValidationException(
                    List.of("Idempotency-Key must contain 1 to 120 non-whitespace characters."));
        }
        if (requests == null || requests.isEmpty()) {
            throw new PaymentImportValidationException(List.of("At least one payment record is required."));
        }
        if (requests.size() > MAX_RECORDS) {
            throw new PaymentImportValidationException(
                    List.of("An import may contain no more than %d records.".formatted(MAX_RECORDS)));
        }

        List<String> errors = new ArrayList<>();
        for (int index = 0; index < requests.size(); index++) {
            PaymentRecordRequest request = requests.get(index);
            if (request == null) {
                errors.add("Record %d must not be null.".formatted(index + 1));
                continue;
            }
            for (ConstraintViolation<PaymentRecordRequest> violation : validator.validate(request)) {
                errors.add("Record %d, %s: %s".formatted(
                        index + 1,
                        violation.getPropertyPath(),
                        violation.getMessage()));
            }
        }
        if (!errors.isEmpty()) {
            throw new PaymentImportValidationException(errors);
        }
    }

    private Map<PaymentKey, PaymentRecord> findExistingRecords(List<PaymentRecordRequest> requests) {
        Map<PaymentSource, Set<String>> referencesBySource = new EnumMap<>(PaymentSource.class);
        for (PaymentRecordRequest request : requests) {
            referencesBySource.computeIfAbsent(request.source(), ignored -> new HashSet<>())
                    .add(request.transactionReference());
        }

        Map<PaymentKey, PaymentRecord> existingByKey = new HashMap<>();
        referencesBySource.forEach((source, references) ->
                paymentRecordRepository.findBySourceAndTransactionReferenceIn(source, references)
                        .forEach(record -> existingByKey.put(PaymentKey.from(record), record)));
        return existingByKey;
    }

    private PaymentRecord toEntity(PaymentRecordRequest request, Instant createdAt) {
        return new PaymentRecord(
                UUID.randomUUID(),
                request.source(),
                request.transactionReference(),
                request.endToEndId(),
                request.amount(),
                request.currency(),
                request.counterparty(),
                request.valueDate(),
                request.status(),
                createdAt,
                false);
    }

    private String hash(List<PaymentRecordRequest> requests) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(objectMapper.writeValueAsBytes(requests));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | JsonProcessingException exception) {
            throw new IllegalStateException("Could not calculate the payment import fingerprint.", exception);
        }
    }

    private String writeResponse(PaymentImportResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not persist the payment import response.", exception);
        }
    }

    private PaymentImportResponse readResponse(String payload) {
        try {
            return objectMapper.readValue(payload, PaymentImportResponse.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not read the persisted payment import response.", exception);
        }
    }

    private record PaymentKey(PaymentSource source, String transactionReference) {

        static PaymentKey from(PaymentRecordRequest request) {
            return new PaymentKey(request.source(), request.transactionReference());
        }

        static PaymentKey from(PaymentRecord record) {
            return new PaymentKey(record.getSource(), record.getTransactionReference());
        }
    }
}
