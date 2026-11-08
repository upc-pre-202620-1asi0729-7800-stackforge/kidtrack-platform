package com.stackforge.kidtrack.platform.shared.interfaces.rest;

import com.stackforge.kidtrack.platform.shared.domain.exceptions.BusinessRuleException;
import com.stackforge.kidtrack.platform.shared.domain.exceptions.ConflictException;
import com.stackforge.kidtrack.platform.shared.domain.exceptions.InvalidCredentialsException;
import com.stackforge.kidtrack.platform.shared.domain.exceptions.NotFoundException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Translates domain and framework exceptions into RFC 7807 problem details responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflict(ConflictException ex) {
        return problem(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCredentials(InvalidCredentialsException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage());
    }

    @ExceptionHandler({BusinessRuleException.class, IllegalArgumentException.class})
    public ResponseEntity<ProblemDetail> handleBadRequest(RuntimeException ex) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_DATA", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        var detail = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", detail);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ProblemDetail> handleUnreadable(Exception ex) {
        return problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "The request could not be read.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleIntegrity(DataIntegrityViolationException ex) {
        LOGGER.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "DATA_INTEGRITY", "The operation violates a data integrity constraint.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        LOGGER.error("Unhandled exception", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred.");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("code", code);
        return ResponseEntity.status(status).body(problem);
    }
}
