package io.cvvexxx.orders.controller;

import io.cvvexxx.orders.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ProblemDetail> handleBindException(BindException ex, Locale locale) {
        log.warn("Validation error occurred: {}", ex.getMessage());

        String title = messageSource.getMessage("errors.400.title", null, "Bad Request", locale);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, title);

        List<String> errors = ex.getAllErrors().stream()
                .map(error -> messageSource.getMessage(error, locale))
                .toList();

        problemDetail.setProperty("errors", errors);
        log.error(problemDetail.toString());
        return ResponseEntity.badRequest().body(problemDetail);
    }


    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleOrderNotFoundException(OrderNotFoundException ex, Locale locale) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildProblemResponse(HttpStatus.NOT_FOUND, ex, locale);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ProblemDetail> handleNoSuchElementException(NoSuchElementException ex, Locale locale) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildProblemResponse(HttpStatus.NOT_FOUND, ex, locale);
    }

    @ExceptionHandler(OrderAccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleOrderAccessDeniedException(OrderAccessDeniedException ex, Locale locale) {
        log.warn("Access denied: {}", ex.getMessage());
        return buildProblemResponse(HttpStatus.FORBIDDEN, ex, locale);
    }

    @ExceptionHandler(OrderCannotCancelException.class)
    public ResponseEntity<ProblemDetail> handleOrderCannotCancelException(OrderCannotCancelException ex, Locale locale) {
        log.warn("Conflict: {}", ex.getMessage());
        return buildProblemResponse(HttpStatus.CONFLICT, ex, locale);
    }

    @ExceptionHandler(OrderCannotConfirmException.class)
    public ResponseEntity<ProblemDetail> handleOrderCannotConfirmException(OrderCannotConfirmException ex, Locale locale) {
        log.warn("Conflict: {}", ex.getMessage());
        return buildProblemResponse(HttpStatus.CONFLICT, ex, locale);
    }


    private ResponseEntity<ProblemDetail> buildProblemResponse(HttpStatus status, Throwable ex, Locale locale) {
        String detail = resolveMessage(ex, locale);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setProperty("errors", List.of(detail));
        log.error(problemDetail.toString());
        return ResponseEntity.status(status).body(problemDetail);
    }

    private String resolveMessage(Throwable ex, Locale locale) {
        Object[] args = ex instanceof LocalizedException le ? le.getArgs() : new Object[0];
        return messageSource.getMessage(ex.getMessage(), args, ex.getMessage(), locale);
    }
}
