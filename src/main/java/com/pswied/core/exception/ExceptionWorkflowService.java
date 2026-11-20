package com.pswied.core.exception;

import com.pswied.core.approval.Approval;
import com.pswied.core.approval.ApprovalDecision;
import com.pswied.core.approval.ApprovalRepository;
import com.pswied.core.approval.ApprovalResponse;
import com.pswied.core.approval.ApprovalTargetType;
import com.pswied.core.audit.AuditLog;
import com.pswied.core.audit.AuditLogRepository;
import com.pswied.core.audit.AuditLogResponse;
import com.pswied.core.investigation.Investigation;
import com.pswied.core.investigation.InvestigationNote;
import com.pswied.core.investigation.InvestigationNoteRepository;
import com.pswied.core.investigation.InvestigationNoteResponse;
import com.pswied.core.investigation.InvestigationRepository;
import com.pswied.core.investigation.InvestigationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ExceptionWorkflowService {

    private static final String AGGREGATE_TYPE = "PAYMENT_EXCEPTION";

    private final PaymentExceptionRepository exceptionRepository;
    private final InvestigationRepository investigationRepository;
    private final InvestigationNoteRepository noteRepository;
    private final ExceptionResolutionRequestRepository resolutionRequestRepository;
    private final ApprovalRepository approvalRepository;
    private final AuditLogRepository auditLogRepository;

    public ExceptionWorkflowService(
            PaymentExceptionRepository exceptionRepository,
            InvestigationRepository investigationRepository,
            InvestigationNoteRepository noteRepository,
            ExceptionResolutionRequestRepository resolutionRequestRepository,
            ApprovalRepository approvalRepository,
            AuditLogRepository auditLogRepository) {
        this.exceptionRepository = exceptionRepository;
        this.investigationRepository = investigationRepository;
        this.noteRepository = noteRepository;
        this.resolutionRequestRepository = resolutionRequestRepository;
        this.approvalRepository = approvalRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public List<PaymentExceptionResponse> list() {
        return exceptionRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(PaymentExceptionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentExceptionResponse get(UUID exceptionId) {
        return PaymentExceptionResponse.from(findException(exceptionId));
    }

    @Transactional
    public PaymentExceptionResponse assign(UUID exceptionId, String assignedTo, String actor) {
        PaymentException exception = findExceptionForUpdate(exceptionId);
        ensureMutable(exception);
        String normalizedAssignee = assignedTo.strip();
        exception.assignTo(normalizedAssignee);
        audit(exceptionId, actor, "ASSIGNED", "assignedTo=%s".formatted(normalizedAssignee));
        return PaymentExceptionResponse.from(exceptionRepository.save(exception));
    }

    @Transactional
    public PaymentExceptionResponse updateSeverity(UUID exceptionId, ExceptionSeverity severity, String actor) {
        PaymentException exception = findExceptionForUpdate(exceptionId);
        ensureMutable(exception);
        int priority = priorityFor(severity);
        exception.updateSeverity(severity, priority);
        audit(exceptionId, actor, "SEVERITY_UPDATED", "severity=%s; priority=%d".formatted(severity, priority));
        return PaymentExceptionResponse.from(exceptionRepository.save(exception));
    }

    @Transactional
    public InvestigationNoteResponse addNote(UUID exceptionId, String note, String actor) {
        PaymentException exception = findExceptionForUpdate(exceptionId);
        ensureMutable(exception);
        Instant now = Instant.now();
        Investigation investigation = investigationRepository.findByExceptionId(exceptionId)
                .orElseGet(() -> new Investigation(
                        UUID.randomUUID(),
                        exceptionId,
                        InvestigationStatus.IN_PROGRESS,
                        null,
                        now,
                        null));
        investigation.markInProgress();
        investigationRepository.save(investigation);
        exception.updateStatus(PaymentExceptionStatus.INVESTIGATING);
        exceptionRepository.save(exception);
        InvestigationNote savedNote = noteRepository.save(new InvestigationNote(
                UUID.randomUUID(), exceptionId, actor, note.strip(), now));
        audit(exceptionId, actor, "INVESTIGATION_NOTE_ADDED", "noteId=%s".formatted(savedNote.getId()));
        return InvestigationNoteResponse.from(savedNote);
    }

    @Transactional(readOnly = true)
    public List<InvestigationNoteResponse> notes(UUID exceptionId) {
        findException(exceptionId);
        return noteRepository.findByExceptionIdOrderByCreatedAtAscIdAsc(exceptionId).stream()
                .map(InvestigationNoteResponse::from)
                .toList();
    }

    @Transactional
    public ExceptionResolutionRequestResponse requestResolution(
            UUID exceptionId,
            ExceptionResolutionAction action,
            String justification,
            String actor) {
        PaymentException exception = findExceptionForUpdate(exceptionId);
        if (resolutionRequestRepository.findFirstByExceptionIdAndStatusOrderByRequestedAtDesc(
                exceptionId, ExceptionResolutionRequestStatus.PENDING).isPresent()) {
            throw new ExceptionWorkflowConflictException("This exception already has a pending approval request.");
        }
        PaymentExceptionStatus returnStatus;
        if (action == ExceptionResolutionAction.RESOLVE) {
            if (exception.getStatus() != PaymentExceptionStatus.INVESTIGATING
                    || noteRepository.findByExceptionIdOrderByCreatedAtAscIdAsc(exceptionId).isEmpty()) {
                throw new ExceptionWorkflowConflictException(
                        "A resolution can be requested only after an investigation note has been recorded.");
            }
            returnStatus = PaymentExceptionStatus.INVESTIGATING;
        } else {
            if (exception.getStatus() != PaymentExceptionStatus.RESOLVED) {
                throw new ExceptionWorkflowConflictException("Only a resolved exception can be submitted for closure.");
            }
            returnStatus = PaymentExceptionStatus.RESOLVED;
        }

        Instant now = Instant.now();
        ExceptionResolutionRequest request = resolutionRequestRepository.save(new ExceptionResolutionRequest(
                UUID.randomUUID(),
                exceptionId,
                action,
                actor,
                justification.strip(),
                returnStatus,
                ExceptionResolutionRequestStatus.PENDING,
                now,
                null));
        exception.updateStatus(PaymentExceptionStatus.PENDING_APPROVAL);
        exceptionRepository.save(exception);
        audit(exceptionId, actor, "RESOLUTION_REQUESTED",
                "requestId=%s; action=%s; justification=%s".formatted(request.getId(), action, justification.strip()));
        return ExceptionResolutionRequestResponse.from(request);
    }

    @Transactional(readOnly = true)
    public List<ExceptionResolutionRequestResponse> pendingApprovals() {
        return resolutionRequestRepository.findAllByStatusOrderByRequestedAtAsc(
                        ExceptionResolutionRequestStatus.PENDING).stream()
                .map(ExceptionResolutionRequestResponse::from)
                .toList();
    }

    @Transactional
    public ApprovalResponse decide(UUID requestId, ApprovalDecision decision, String justification, String actor) {
        ExceptionResolutionRequest request = resolutionRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ExceptionResolutionRequestNotFoundException(requestId));
        if (request.getStatus() != ExceptionResolutionRequestStatus.PENDING) {
            throw new ExceptionWorkflowConflictException("This approval request has already been decided.");
        }
        if (request.getRequestedBy().equals(actor)) {
            throw new ExceptionWorkflowConflictException("The requester cannot approve or reject their own request.");
        }

        PaymentException exception = findExceptionForUpdate(request.getExceptionId());
        Instant now = Instant.now();
        request.decide(
                decision == ApprovalDecision.APPROVED
                        ? ExceptionResolutionRequestStatus.APPROVED
                        : ExceptionResolutionRequestStatus.REJECTED,
                now);
        resolutionRequestRepository.save(request);

        if (decision == ApprovalDecision.APPROVED) {
            PaymentExceptionStatus newStatus = request.getAction() == ExceptionResolutionAction.RESOLVE
                    ? PaymentExceptionStatus.RESOLVED
                    : PaymentExceptionStatus.CLOSED;
            exception.updateStatus(newStatus);
            investigationRepository.findByExceptionId(exception.getId())
                    .ifPresent(investigation -> {
                        investigation.complete(now);
                        investigationRepository.save(investigation);
                    });
        } else {
            exception.updateStatus(request.getReturnStatus());
        }
        exceptionRepository.save(exception);

        Approval approval = approvalRepository.save(new Approval(
                UUID.randomUUID(),
                ApprovalTargetType.PAYMENT_EXCEPTION_RESOLUTION,
                exception.getId(),
                decision,
                actor,
                justification.strip(),
                now));
        audit(exception.getId(), actor,
                decision == ApprovalDecision.APPROVED ? "RESOLUTION_APPROVED" : "RESOLUTION_REJECTED",
                "requestId=%s; decision=%s; justification=%s".formatted(requestId, decision, justification.strip()));
        return ApprovalResponse.from(approval);
    }

    @Transactional(readOnly = true)
    public List<ApprovalResponse> approvals(UUID exceptionId) {
        findException(exceptionId);
        return approvalRepository.findByTargetTypeAndTargetIdOrderByDecidedAtAsc(
                        ApprovalTargetType.PAYMENT_EXCEPTION_RESOLUTION, exceptionId)
                .stream()
                .map(ApprovalResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> auditHistory(UUID exceptionId) {
        findException(exceptionId);
        return auditLogRepository.findByAggregateTypeAndAggregateIdOrderByOccurredAtAscIdAsc(
                        AGGREGATE_TYPE, exceptionId)
                .stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    private PaymentException findException(UUID exceptionId) {
        return exceptionRepository.findById(exceptionId)
                .orElseThrow(() -> new PaymentExceptionNotFoundException(exceptionId));
    }

    private PaymentException findExceptionForUpdate(UUID exceptionId) {
        return exceptionRepository.findByIdForUpdate(exceptionId)
                .orElseThrow(() -> new PaymentExceptionNotFoundException(exceptionId));
    }

    private void ensureMutable(PaymentException exception) {
        if (exception.getStatus() == PaymentExceptionStatus.PENDING_APPROVAL
                || exception.getStatus() == PaymentExceptionStatus.RESOLVED
                || exception.getStatus() == PaymentExceptionStatus.REJECTED
                || exception.getStatus() == PaymentExceptionStatus.CLOSED) {
            throw new ExceptionWorkflowConflictException(
                    "This exception cannot be changed while it is %s.".formatted(exception.getStatus()));
        }
    }

    private int priorityFor(ExceptionSeverity severity) {
        return switch (severity) {
            case CRITICAL -> 1;
            case HIGH -> 2;
            case MEDIUM -> 3;
            case LOW -> 4;
        };
    }

    private void audit(UUID exceptionId, String actor, String action, String details) {
        auditLogRepository.save(new AuditLog(
                UUID.randomUUID(),
                actor,
                action,
                AGGREGATE_TYPE,
                exceptionId,
                details,
                Instant.now()));
    }
}
