package com.pswied.core.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "payment_record", uniqueConstraints = {
        @UniqueConstraint(name = "uq_payment_record_source_reference",
                columnNames = {"source", "transaction_reference"})
})
public class PaymentRecord {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentSource source;

    @Column(name = "transaction_reference", nullable = false, length = 100)
    private String transactionReference;

    @Column(name = "end_to_end_id", length = 100)
    private String endToEndId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 140)
    private String counterparty;

    @Column(name = "value_date", nullable = false)
    private LocalDate valueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PaymentStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "is_demo", nullable = false)
    private boolean demoRecord;

    protected PaymentRecord() {
    }

    public PaymentRecord(
            UUID id,
            PaymentSource source,
            String transactionReference,
            String endToEndId,
            BigDecimal amount,
            String currency,
            String counterparty,
            LocalDate valueDate,
            PaymentStatus status,
            Instant createdAt,
            boolean demoRecord) {
        this.id = id;
        this.source = source;
        this.transactionReference = transactionReference;
        this.endToEndId = endToEndId;
        this.amount = amount;
        this.currency = currency;
        this.counterparty = counterparty;
        this.valueDate = valueDate;
        this.status = status;
        this.createdAt = createdAt;
        this.demoRecord = demoRecord;
    }

    public UUID getId() {
        return id;
    }

    public PaymentSource getSource() {
        return source;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public String getEndToEndId() {
        return endToEndId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getCounterparty() {
        return counterparty;
    }

    public LocalDate getValueDate() {
        return valueDate;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isDemoRecord() {
        return demoRecord;
    }
}
