package com.pswied.core.api;

import com.pswied.core.payment.DuplicatePaymentRecordException;
import com.pswied.core.payment.PaymentImportIdempotencyConflictException;
import com.pswied.core.payment.PaymentImportValidationException;
import com.pswied.core.payment.PaymentRecordNotFoundException;
import com.pswied.core.network.NetworkSubmissionNotFoundException;
import com.pswied.core.reconciliation.ReconciliationBatchNotFoundException;
import com.pswied.core.exception.ExceptionWorkflowConflictException;
import com.pswied.core.exception.PaymentExceptionNotFoundException;
import com.pswied.core.exception.ExceptionResolutionRequestNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(PaymentRecordNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleNotFound(PaymentRecordNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(NetworkSubmissionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleSubmissionNotFound(NetworkSubmissionNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(ReconciliationBatchNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleReconciliationBatchNotFound(ReconciliationBatchNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(PaymentExceptionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handlePaymentExceptionNotFound(PaymentExceptionNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(ExceptionWorkflowConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleExceptionWorkflowConflict(ExceptionWorkflowConflictException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(ExceptionResolutionRequestNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleResolutionRequestNotFound(ExceptionResolutionRequestNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(DuplicatePaymentRecordException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleDuplicate(DuplicatePaymentRecordException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(PaymentImportIdempotencyConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleIdempotencyConflict(PaymentImportIdempotencyConflictException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(PaymentImportValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleImportValidation(PaymentImportValidationException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setProperty("errors", exception.getErrors());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleInvalidRequest(MethodArgumentNotValidException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "The request contains invalid fields.");
        problem.setProperty("errors", exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList());
        return problem;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleConstraintViolation() {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "The request conflicts with an existing record or database constraint.");
    }
}
