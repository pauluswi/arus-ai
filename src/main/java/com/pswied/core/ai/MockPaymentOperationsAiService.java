package com.pswied.core.ai;

import com.pswied.core.exception.PaymentExceptionCategory;
import com.pswied.core.payment.PaymentRecord;
import com.pswied.core.payment.PaymentRecordNotFoundException;
import com.pswied.core.payment.PaymentRecordRepository;
import com.pswied.core.payment.PaymentSource;
import com.pswied.core.payment.PaymentStatus;
import com.pswied.core.reconciliation.ReconciliationResult;
import com.pswied.core.reconciliation.ReconciliationResultNotFoundException;
import com.pswied.core.reconciliation.ReconciliationResultRepository;
import com.pswied.core.reconciliation.ReconciliationResultStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Profile({"local", "test"})
public class MockPaymentOperationsAiService implements PaymentOperationsAiService {

    private static final String DISCLAIMER =
            "Deterministic mock output for demonstration only; not a calibrated model prediction or an instruction to act.";
    private static final BigDecimal MINIMUM_MATCH_SCORE = new BigDecimal("0.30");
    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("10000.00");
    private static final int MAX_MATCH_CANDIDATES = 5;

    private static final List<KnowledgeFixture> KNOWLEDGE = List.of(
            new KnowledgeFixture(
                    "reconciliation-matching",
                    "Reconciliation and match review",
                    "Compare transaction references first, then end-to-end identifiers and settlement details. "
                            + "Ambiguous or unmatched records require an operations review.",
                    List.of("reconciliation", "match", "matching", "unmatched", "reference", "settlement")),
            new KnowledgeFixture(
                    "payment-failure",
                    "Payment failure triage",
                    "Review the payment status, network response, and reason code. Do not retry or resolve a "
                            + "failed payment without an authorized human decision.",
                    List.of("failure", "failed", "rejected", "retry", "reason", "network")),
            new KnowledgeFixture(
                    "exception-approval",
                    "Exception approval controls",
                    "Record investigation evidence before requesting resolution. A distinct authorized reviewer "
                            + "must approve sensitive actions and provide a justification.",
                    List.of("exception", "approval", "approve", "resolve", "close", "authorization")),
            new KnowledgeFixture(
                    "anomaly-review",
                    "Anomaly review guidance",
                    "Treat unusual amounts and failed statuses as review signals, not proof of fraud. Validate "
                            + "against the original instruction and independent evidence.",
                    List.of("anomaly", "unusual", "amount", "fraud", "review", "evidence")),
            new KnowledgeFixture(
                    "audit-evidence",
                    "Investigation evidence and audit",
                    "Keep source records, reconciliation evidence, investigation notes, and approval decisions "
                            + "traceable in the case audit history.",
                    List.of("audit", "evidence", "investigation", "notes", "traceability", "history")));

    private final PaymentRecordRepository paymentRecordRepository;
    private final ReconciliationResultRepository reconciliationResultRepository;

    public MockPaymentOperationsAiService(
            PaymentRecordRepository paymentRecordRepository,
            ReconciliationResultRepository reconciliationResultRepository) {
        this.paymentRecordRepository = paymentRecordRepository;
        this.reconciliationResultRepository = reconciliationResultRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchCandidate> rankCandidates(UUID paymentRecordId) {
        PaymentRecord source = findPaymentRecord(paymentRecordId);
        PaymentSource candidateSource = source.getSource() == PaymentSource.INTERNAL
                ? PaymentSource.EXTERNAL
                : PaymentSource.INTERNAL;
        return paymentRecordRepository.findBySourceOrderByValueDateAscTransactionReferenceAscIdAsc(candidateSource)
                .stream()
                .map(candidate -> candidateFor(source, candidate))
                .filter(candidate -> candidate.score().compareTo(MINIMUM_MATCH_SCORE) >= 0)
                .sorted(Comparator.comparing(MatchCandidate::score).reversed()
                        .thenComparing(MatchCandidate::paymentRecordId))
                .limit(MAX_MATCH_CANDIDATES)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AnomalyReport detectAnomalies(UUID paymentRecordId) {
        PaymentRecord record = findPaymentRecord(paymentRecordId);
        List<String> indicators = new ArrayList<>();
        BigDecimal score = new BigDecimal("0.05");
        if (record.getStatus() == PaymentStatus.FAILED || record.getStatus() == PaymentStatus.REJECTED) {
            indicators.add("Payment status is " + record.getStatus() + ".");
            score = score.add(new BigDecimal("0.65"));
        }
        if (record.getAmount().compareTo(HIGH_VALUE_THRESHOLD) >= 0) {
            indicators.add("Amount meets or exceeds the illustrative high-value threshold of "
                    + HIGH_VALUE_THRESHOLD.toPlainString() + " " + record.getCurrency() + ".");
            score = score.add(new BigDecimal("0.35"));
        }
        score = score.min(BigDecimal.ONE).setScale(2, RoundingMode.HALF_UP);
        String severity = score.compareTo(new BigDecimal("0.70")) >= 0 ? "HIGH"
                : score.compareTo(new BigDecimal("0.35")) >= 0 ? "MEDIUM" : "LOW";
        if (indicators.isEmpty()) {
            indicators.add("No configured mock anomaly signals were found.");
        }
        return new AnomalyReport(
                record.getId(), "payment-anomaly-rules-v1", score, severity, List.copyOf(indicators), DISCLAIMER);
    }

    @Override
    @Transactional(readOnly = true)
    public ExceptionClassification classifyException(UUID reconciliationResultId) {
        ReconciliationResult result = reconciliationResultRepository.findById(reconciliationResultId)
                .orElseThrow(() -> new ReconciliationResultNotFoundException(reconciliationResultId));
        if (result.getStatus() == ReconciliationResultStatus.MATCHED) {
            throw new MatchedResultClassificationException(reconciliationResultId);
        }
        PaymentExceptionCategory category = classify(result);
        BigDecimal confidence = switch (category) {
            case PAYMENT_FAILURE -> new BigDecimal("0.94");
            case AMOUNT_MISMATCH, CURRENCY_MISMATCH -> new BigDecimal("0.92");
            case UNMATCHED_TRANSACTION, DUPLICATE_TRANSACTION -> new BigDecimal("0.88");
            default -> new BigDecimal("0.78");
        };
        return new ExceptionClassification(
                result.getId(),
                result.getStatus(),
                category,
                confidence,
                "exception-classification-rules-v1",
                "Category selected from the persisted reconciliation status, matching evidence, and payment statuses.",
                DISCLAIMER);
    }

    @Override
    @Transactional(readOnly = true)
    public FailurePrediction predictFailure(UUID paymentRecordId) {
        PaymentRecord record = findPaymentRecord(paymentRecordId);
        return switch (record.getStatus()) {
            case FAILED, REJECTED -> new FailurePrediction(
                    record.getId(), record.getStatus(), "HIGH", new BigDecimal("0.94"),
                    "payment-failure-status-v1",
                    "The payment is already marked " + record.getStatus() + "; this is a status-based fixture, not a forward-looking forecast.",
                    DISCLAIMER);
            case PENDING -> new FailurePrediction(
                    record.getId(), record.getStatus(), "MEDIUM", new BigDecimal("0.62"),
                    "payment-failure-pending-v1",
                    "Pending status selects the illustrative medium-risk fixture.",
                    DISCLAIMER);
            case RECEIVED -> new FailurePrediction(
                    record.getId(), record.getStatus(), "LOW", new BigDecimal("0.28"),
                    "payment-failure-received-v1",
                    "Received status selects the illustrative low-risk fixture.",
                    DISCLAIMER);
            case SETTLED -> new FailurePrediction(
                    record.getId(), record.getStatus(), "LOW", new BigDecimal("0.08"),
                    "payment-failure-settled-v1",
                    "Settled status selects the illustrative low-risk fixture.",
                    DISCLAIMER);
        };
    }

    @Override
    public KnowledgeSearchResult searchKnowledge(String query) {
        String normalizedQuery = query.strip().toLowerCase(Locale.ROOT);
        Set<String> queryTerms = new LinkedHashSet<>(Arrays.asList(normalizedQuery.split("[^a-z0-9]+")));
        queryTerms.removeIf(term -> term.length() < 3);
        List<KnowledgeArticle> results = KNOWLEDGE.stream()
                .map(fixture -> articleFor(fixture, queryTerms))
                .filter(article -> !article.matchedTerms().isEmpty())
                .sorted(Comparator.comparing(KnowledgeArticle::relevance).reversed()
                        .thenComparing(KnowledgeArticle::id))
                .limit(3)
                .toList();
        return new KnowledgeSearchResult(query, "mock-knowledge-fixtures", results, DISCLAIMER);
    }

    private PaymentRecord findPaymentRecord(UUID id) {
        return paymentRecordRepository.findById(id).orElseThrow(() -> new PaymentRecordNotFoundException(id));
    }

    private MatchCandidate candidateFor(PaymentRecord source, PaymentRecord candidate) {
        List<String> signals = new ArrayList<>();
        BigDecimal score = BigDecimal.ZERO;
        if (sameNonBlank(source.getTransactionReference(), candidate.getTransactionReference())) {
            signals.add("transactionReference");
            score = score.add(new BigDecimal("0.40"));
        }
        if (sameNonBlank(source.getEndToEndId(), candidate.getEndToEndId())) {
            signals.add("endToEndId");
            score = score.add(new BigDecimal("0.30"));
        }
        if (source.getAmount().compareTo(candidate.getAmount()) == 0) {
            signals.add("amount");
            score = score.add(new BigDecimal("0.10"));
        }
        if (source.getCurrency().equals(candidate.getCurrency())) {
            signals.add("currency");
            score = score.add(new BigDecimal("0.08"));
        }
        if (source.getValueDate().equals(candidate.getValueDate())) {
            signals.add("valueDate");
            score = score.add(new BigDecimal("0.06"));
        }
        if (source.getCounterparty().strip().equalsIgnoreCase(candidate.getCounterparty().strip())) {
            signals.add("counterparty");
            score = score.add(new BigDecimal("0.06"));
        }
        score = score.setScale(2, RoundingMode.HALF_UP);
        String confidence = score.compareTo(new BigDecimal("0.80")) >= 0 ? "HIGH"
                : score.compareTo(new BigDecimal("0.50")) >= 0 ? "MEDIUM" : "LOW";
        return new MatchCandidate(
                candidate.getId(),
                candidate.getTransactionReference(),
                candidate.getCounterparty(),
                candidate.getAmount(),
                candidate.getCurrency(),
                score,
                confidence,
                List.copyOf(signals));
    }

    private PaymentExceptionCategory classify(ReconciliationResult result) {
        if (result.getStatus() == ReconciliationResultStatus.UNMATCHED) {
            return PaymentExceptionCategory.UNMATCHED_TRANSACTION;
        }
        if (result.getStatus() == ReconciliationResultStatus.DUPLICATE) {
            return PaymentExceptionCategory.DUPLICATE_TRANSACTION;
        }
        String evidence = result.getMatchingEvidence().toLowerCase(Locale.ROOT);
        List<PaymentRecord> records = paymentRecordRepository.findAllById(
                List.of(result.getInternalRecordId(), result.getExternalRecordId()));
        boolean failedPayment = records.stream()
                .anyMatch(record -> record.getStatus() == PaymentStatus.FAILED
                        || record.getStatus() == PaymentStatus.REJECTED);
        if (failedPayment || evidence.contains("paymentstatus")) {
            return PaymentExceptionCategory.PAYMENT_FAILURE;
        }
        if (evidence.contains("amount")) {
            return PaymentExceptionCategory.AMOUNT_MISMATCH;
        }
        if (evidence.contains("currency")) {
            return PaymentExceptionCategory.CURRENCY_MISMATCH;
        }
        return PaymentExceptionCategory.SETTLEMENT_DISCREPANCY;
    }

    private KnowledgeArticle articleFor(KnowledgeFixture fixture, Set<String> queryTerms) {
        List<String> matchedTerms = fixture.terms().stream()
                .filter(term -> queryTerms.stream().anyMatch(queryTerm ->
                        term.contains(queryTerm) || queryTerm.contains(term)))
                .distinct()
                .toList();
        BigDecimal relevance = matchedTerms.isEmpty()
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(matchedTerms.size())
                        .divide(BigDecimal.valueOf(fixture.terms().size()), 2, RoundingMode.HALF_UP);
        return new KnowledgeArticle(
                fixture.id(), fixture.title(), fixture.summary(), matchedTerms, relevance);
    }

    private boolean sameNonBlank(String left, String right) {
        return left != null && !left.isBlank() && left.equalsIgnoreCase(right);
    }

    private record KnowledgeFixture(String id, String title, String summary, List<String> terms) {
    }
}
