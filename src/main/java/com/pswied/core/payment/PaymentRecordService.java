package com.pswied.core.payment;

import org.springframework.data.domain.Sort;
import com.pswied.core.config.DomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentRecordService {

    private final PaymentRecordRepository repository;
    private final DomainEventPublisher eventPublisher;

    public PaymentRecordService(
            PaymentRecordRepository repository,
            DomainEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
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
        PaymentRecord saved = repository.save(record);
        eventPublisher.publish(new PaymentRecordsIngestedEvent(
                UUID.randomUUID(),
                List.of(saved.getId()),
                Instant.now()));
        return PaymentRecordResponse.from(saved);
    }
}
