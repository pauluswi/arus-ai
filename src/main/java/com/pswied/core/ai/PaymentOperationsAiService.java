package com.pswied.core.ai;

import com.pswied.core.exception.PaymentExceptionCategory;
import com.pswied.core.payment.PaymentStatus;
import com.pswied.core.reconciliation.ReconciliationResultStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PaymentOperationsAiService {

    List<MatchCandidate> rankCandidates(UUID paymentRecordId);

    AnomalyReport detectAnomalies(UUID paymentRecordId);

    ExceptionClassification classifyException(UUID reconciliationResultId);

    FailurePrediction predictFailure(UUID paymentRecordId);

    KnowledgeSearchResult searchKnowledge(String query);

    record MatchCandidate(
            UUID paymentRecordId,
            String transactionReference,
            String counterparty,
            BigDecimal amount,
            String currency,
            BigDecimal score,
            String confidence,
            List<String> matchingSignals) {
    }

    record AnomalyReport(
            UUID paymentRecordId,
            String fixtureId,
            BigDecimal score,
            String severity,
            List<String> indicators,
            String disclaimer) {
    }

    record ExceptionClassification(
            UUID reconciliationResultId,
            ReconciliationResultStatus reconciliationStatus,
            PaymentExceptionCategory category,
            BigDecimal confidence,
            String fixtureId,
            String rationale,
            String disclaimer) {
    }

    record FailurePrediction(
            UUID paymentRecordId,
            PaymentStatus currentStatus,
            String risk,
            BigDecimal score,
            String fixtureId,
            String rationale,
            String disclaimer) {
    }

    record KnowledgeSearchResult(
            String query,
            String provider,
            List<KnowledgeArticle> results,
            String disclaimer) {
    }

    record KnowledgeArticle(
            String id,
            String title,
            String summary,
            List<String> matchedTerms,
            BigDecimal relevance) {
    }
}
