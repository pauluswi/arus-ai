package com.pswied.core.ai;

import com.pswied.core.exception.PaymentException;
import com.pswied.core.exception.PaymentExceptionCategory;
import com.pswied.core.exception.PaymentExceptionNotFoundException;
import com.pswied.core.exception.PaymentExceptionRepository;
import com.pswied.core.investigation.InvestigationNote;
import com.pswied.core.investigation.InvestigationNoteRepository;
import com.pswied.core.payment.PaymentRecord;
import com.pswied.core.payment.PaymentRecordNotFoundException;
import com.pswied.core.payment.PaymentRecordRepository;
import com.pswied.core.reconciliation.ReconciliationResult;
import com.pswied.core.reconciliation.ReconciliationResultNotFoundException;
import com.pswied.core.reconciliation.ReconciliationResultRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Profile({"local", "test"})
public class MockInvestigationAiService implements InvestigationAiService {

    private static final String PROVIDER = "mock-fixture";
    private static final String DISCLAIMER =
            "Fixture-based demonstration output; recommendations are not authoritative and require human review.";

    private static final Map<PaymentExceptionCategory, Fixture> FIXTURES = Map.of(
            PaymentExceptionCategory.UNMATCHED_TRANSACTION,
            new Fixture(
                    "unmatched-transaction-v1",
                    "A payment record has no unique counterpart in the reconciled source.",
                    confidence("0.82", "MEDIUM"),
                    List.of(
                            new RecommendationFixture(
                                    "Review the source record and confirm settlement status with the counterparty.",
                                    "The reconciliation result contains an unmatched payment record.",
                                    confidence("0.79", "MEDIUM")),
                            new RecommendationFixture(
                                    "Keep the exception open until supporting settlement evidence is recorded.",
                                    "No matching counterpart is available to support automatic resolution.",
                                    confidence("0.86", "MEDIUM")))),
            PaymentExceptionCategory.AMOUNT_MISMATCH,
            new Fixture(
                    "amount-mismatch-v1",
                    "Matched payment records report different amounts.",
                    confidence("0.91", "HIGH"),
                    List.of(
                            new RecommendationFixture(
                                    "Compare the booked amount with the external settlement record.",
                                    "The reconciliation evidence identifies an amount difference.",
                                    confidence("0.90", "HIGH")),
                            new RecommendationFixture(
                                    "Request independent approval before resolving or closing the exception.",
                                    "The amount difference may affect the settled value.",
                                    confidence("0.88", "HIGH")))),
            PaymentExceptionCategory.CURRENCY_MISMATCH,
            new Fixture(
                    "currency-mismatch-v1",
                    "Matched payment records report different currencies.",
                    confidence("0.93", "HIGH"),
                    List.of(
                            new RecommendationFixture(
                                    "Verify the original payment instruction and settlement currency.",
                                    "The reconciliation evidence identifies a currency difference.",
                                    confidence("0.92", "HIGH")),
                            new RecommendationFixture(
                                    "Do not apply a conversion or correction without an authorized review.",
                                    "The mock service does not have approved FX or correction evidence.",
                                    confidence("0.89", "HIGH")))),
            PaymentExceptionCategory.PAYMENT_FAILURE,
            new Fixture(
                    "payment-failure-v1",
                    "The matched payment records contain a failed or rejected payment status.",
                    confidence("0.89", "HIGH"),
                    List.of(
                            new RecommendationFixture(
                                    "Review both payment statuses and obtain the network or ledger reason code.",
                                    "The reconciliation evidence flags a failed or rejected status.",
                                    confidence("0.87", "HIGH")),
                            new RecommendationFixture(
                                    "Require an authorized human decision before retrying or resolving the payment.",
                                    "A payment retry or resolution must not be initiated automatically.",
                                    confidence("0.94", "HIGH")))),
            PaymentExceptionCategory.SETTLEMENT_DISCREPANCY,
            new Fixture(
                    "settlement-discrepancy-v1",
                    "Matched payment records contain a non-amount settlement difference.",
                    confidence("0.78", "MEDIUM"),
                    List.of(
                            new RecommendationFixture(
                                    "Review the reconciliation evidence and confirm value date and counterparty details.",
                                    "The identifier matched, but one or more settlement fields differ.",
                                    confidence("0.76", "MEDIUM")),
                            new RecommendationFixture(
                                    "Record additional source evidence before requesting resolution.",
                                    "The current report relies only on persisted reconciliation and investigation data.",
                                    confidence("0.84", "MEDIUM")))),
            PaymentExceptionCategory.DUPLICATE_TRANSACTION,
            new Fixture(
                    "duplicate-transaction-v1",
                    "The exception is classified as a duplicate transaction.",
                    confidence("0.86", "MEDIUM"),
                    List.of(
                            new RecommendationFixture(
                                    "Compare source references and payment identifiers before taking action.",
                                    "The exception category indicates a potential duplicate.",
                                    confidence("0.84", "MEDIUM")),
                            new RecommendationFixture(
                                    "Do not remove or reverse a record without independent approval.",
                                    "The mock service cannot determine whether either record is authoritative.",
                                    confidence("0.91", "HIGH")))),
            PaymentExceptionCategory.DELAYED_SETTLEMENT,
            new Fixture(
                    "delayed-settlement-v1",
                    "The exception is classified as a delayed settlement.",
                    confidence("0.77", "MEDIUM"),
                    List.of(
                            new RecommendationFixture(
                                    "Confirm the expected settlement window and inspect available investigation notes.",
                                    "The exception category indicates a settlement delay.",
                                    confidence("0.75", "MEDIUM")),
                            new RecommendationFixture(
                                    "Keep the case open until settlement or a counterparty response is evidenced.",
                                    "No external settlement status is queried by this mock service.",
                                    confidence("0.83", "MEDIUM")))),
            PaymentExceptionCategory.MISSING_TRANSACTION,
            new Fixture(
                    "missing-transaction-v1",
                    "The exception is classified as a missing transaction.",
                    confidence("0.81", "MEDIUM"),
                    List.of(
                            new RecommendationFixture(
                                    "Verify the expected source feed and search for the transaction reference.",
                                    "The exception category indicates a missing transaction.",
                                    confidence("0.79", "MEDIUM")),
                            new RecommendationFixture(
                                    "Request a corrected source file or counterparty confirmation if the record remains absent.",
                                    "The mock service cannot retrieve external source data.",
                                    confidence("0.85", "MEDIUM")))),
            PaymentExceptionCategory.INVALID_REFERENCE,
            new Fixture(
                    "invalid-reference-v1",
                    "The exception is classified as an invalid transaction reference.",
                    confidence("0.88", "HIGH"),
                    List.of(
                            new RecommendationFixture(
                                    "Check the source reference against the originating payment instruction.",
                                    "The exception category indicates an invalid reference.",
                                    confidence("0.86", "MEDIUM")),
                            new RecommendationFixture(
                                    "Preserve the original reference and record any verified correction in the audit trail.",
                                    "The mock service does not modify payment records.",
                                    confidence("0.91", "HIGH")))));

    private final PaymentExceptionRepository exceptionRepository;
    private final ReconciliationResultRepository resultRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final InvestigationNoteRepository noteRepository;

    public MockInvestigationAiService(
            PaymentExceptionRepository exceptionRepository,
            ReconciliationResultRepository resultRepository,
            PaymentRecordRepository paymentRecordRepository,
            InvestigationNoteRepository noteRepository) {
        this.exceptionRepository = exceptionRepository;
        this.resultRepository = resultRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.noteRepository = noteRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public InvestigationReport generateReport(UUID exceptionId) {
        PaymentException exception = exceptionRepository.findById(exceptionId)
                .orElseThrow(() -> new PaymentExceptionNotFoundException(exceptionId));
        Fixture fixture = FIXTURES.get(exception.getCategory());
        ReconciliationResult result = resultRepository.findById(exception.getReconciliationResultId())
                .orElseThrow(() -> new ReconciliationResultNotFoundException(
                        exception.getReconciliationResultId()));
        List<PaymentRecord> paymentRecords = findPaymentRecords(result);
        List<InvestigationNote> notes = noteRepository.findByExceptionIdOrderByCreatedAtAscIdAsc(exceptionId);

        List<InvestigationEvidenceReference> evidence = new ArrayList<>();
        evidence.add(new InvestigationEvidenceReference(
                InvestigationEvidenceType.PAYMENT_EXCEPTION,
                exception.getId(),
                "category=%s; severity=%s; status=%s".formatted(
                        exception.getCategory(), exception.getSeverity(), exception.getStatus())));
        evidence.add(new InvestigationEvidenceReference(
                InvestigationEvidenceType.RECONCILIATION_RESULT,
                result.getId(),
                "status=%s; %s".formatted(result.getStatus(), result.getMatchingEvidence())));
        paymentRecords.forEach(record -> evidence.add(new InvestigationEvidenceReference(
                InvestigationEvidenceType.PAYMENT_RECORD,
                record.getId(),
                "%s reference=%s; endToEndId=%s; amount=%s %s; valueDate=%s; status=%s".formatted(
                        record.getSource(),
                        record.getTransactionReference(),
                        record.getEndToEndId() == null ? "none" : record.getEndToEndId(),
                        record.getAmount().toPlainString(),
                        record.getCurrency(),
                        record.getValueDate(),
                        record.getStatus()))));
        notes.forEach(note -> evidence.add(new InvestigationEvidenceReference(
                InvestigationEvidenceType.INVESTIGATION_NOTE,
                note.getId(),
                note.getNote())));

        List<String> evidenceIds = evidence.stream()
                .map(reference -> reference.type() + ":" + reference.id())
                .toList();
        List<InvestigationRecommendation> recommendations = fixture.recommendations().stream()
                .map(recommendation -> new InvestigationRecommendation(
                        recommendation.action(),
                        recommendation.rationale(),
                        recommendation.confidence(),
                        evidenceIds))
                .toList();

        return new InvestigationReport(
                exception.getId(),
                exception.getCategory(),
                PROVIDER,
                fixture.id(),
                fixture.summary(),
                fixture.confidence(),
                recommendations,
                evidence,
                DISCLAIMER);
    }

    private List<PaymentRecord> findPaymentRecords(ReconciliationResult result) {
        List<UUID> recordIds = new ArrayList<>();
        if (result.getInternalRecordId() != null) {
            recordIds.add(result.getInternalRecordId());
        }
        if (result.getExternalRecordId() != null) {
            recordIds.add(result.getExternalRecordId());
        }
        Map<UUID, PaymentRecord> recordsById = paymentRecordRepository.findAllById(recordIds).stream()
                .collect(java.util.stream.Collectors.toMap(PaymentRecord::getId, record -> record));
        return recordIds.stream()
                .map(recordId -> recordsById.computeIfAbsent(recordId, missingId -> {
                    throw new PaymentRecordNotFoundException(missingId);
                }))
                .toList();
    }

    private static ConfidenceIndicator confidence(String score, String label) {
        return new ConfidenceIndicator(new BigDecimal(score), label);
    }

    private record Fixture(
            String id,
            String summary,
            ConfidenceIndicator confidence,
            List<RecommendationFixture> recommendations) {
    }

    private record RecommendationFixture(
            String action,
            String rationale,
            ConfidenceIndicator confidence) {
    }
}
