package com.pswied.core.payment;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentRecordService {

    private final PaymentRecordRepository repository;

    public PaymentRecordService(PaymentRecordRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<PaymentRecordResponse> list() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "valueDate")
                        .and(Sort.by(Sort.Direction.ASC, "transactionReference")))
                .stream()
                .map(PaymentRecordResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentRecordResponse get(UUID id) {
        return repository.findById(id)
                .map(PaymentRecordResponse::from)
                .orElseThrow(() -> new PaymentRecordNotFoundException(id));
    }

    @Transactional
    public PaymentRecordResponse create(PaymentRecordRequest request) {
        if (repository.existsBySourceAndTransactionReference(request.source(), request.transactionReference())) {
            throw new DuplicatePaymentRecordException(request.source(), request.transactionReference());
        }

        PaymentRecord record = new PaymentRecord(
                UUID.randomUUID(),
                request.source(),
                request.transactionReference(),
                request.endToEndId(),
                request.amount(),
                request.currency(),
                request.counterparty(),
                request.valueDate(),
                request.status(),
                Instant.now(),
                false);
        return PaymentRecordResponse.from(repository.save(record));
    }
}
