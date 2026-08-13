package io.cvvexxx.orders.controller;

import io.cvvexxx.orders.exception.LocalizedException;
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
        return ResponseEntity.badRequest().body(problemDetail);
    }


    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalStateException(IllegalStateException ex, Locale locale) {
        log.warn("Illegal state exception: {}", ex.getMessage());
        return buildProblemResponse(HttpStatus.CONFLICT, ex, locale);
    }


    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ProblemDetail> handleNoSuchElementException(NoSuchElementException ex, Locale locale) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildProblemResponse(HttpStatus.NOT_FOUND, ex, locale);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDeniedException(AccessDeniedException ex, Locale locale) {
        log.warn("Access denied: {}", ex.getMessage());
        return buildProblemResponse(HttpStatus.FORBIDDEN, ex, locale);
    }


    private ResponseEntity<ProblemDetail> buildProblemResponse(HttpStatus status, Throwable ex, Locale locale) {
        String detail = resolveMessage(ex, locale);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        return ResponseEntity.status(status).body(problemDetail);
    }

    private String resolveMessage(Throwable ex, Locale locale) {
        Object[] args = ex instanceof LocalizedException le ? le.getArgs() : new Object[0];
        return messageSource.getMessage(ex.getMessage(), args, ex.getMessage(), locale);
    }
}
