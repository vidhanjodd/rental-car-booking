package com.rentalcar.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── Validation errors ──────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = error instanceof FieldError fe ? fe.getField() : error.getObjectName();
            fieldErrors.put(field, error.getDefaultMessage());
        });
        var body = ErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .error("VALIDATION_FAILED")
            .message("Request validation failed")
            .path(request.getDescription(false))
            .timestamp(Instant.now())
            .fieldErrors(fieldErrors)
            .build();
        return ResponseEntity.badRequest().body(body);
    }

    // ── Domain exceptions ──────────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex, WebRequest req) {
        return buildResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), req);
    }

    @ExceptionHandler(CarNotAvailableException.class)
    public ResponseEntity<ErrorResponse> handleCarNotAvailable(
            CarNotAvailableException ex, WebRequest req) {
        return buildResponse(HttpStatus.CONFLICT, "CAR_NOT_AVAILABLE", ex.getMessage(), req);
    }

    @ExceptionHandler(BookingDateConflictException.class)
    public ResponseEntity<ErrorResponse> handleDateConflict(
            BookingDateConflictException ex, WebRequest req) {
        return buildResponse(HttpStatus.CONFLICT, "BOOKING_DATE_CONFLICT", ex.getMessage(), req);
    }

    @ExceptionHandler(InvalidBookingStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidState(
            InvalidBookingStateException ex, WebRequest req) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_STATE_TRANSITION", ex.getMessage(), req);
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyExists(
            ResourceAlreadyExistsException ex, WebRequest req) {
        return buildResponse(HttpStatus.CONFLICT, "ALREADY_EXISTS", ex.getMessage(), req);
    }

    // ── Auth / Security ────────────────────────────────────────────────────

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(
            InvalidTokenException ex, WebRequest req) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", ex.getMessage(), req);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, WebRequest req) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "Invalid username or password", req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, WebRequest req) {
        return buildResponse(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You don't have permission to access this resource", req);
    }

    // ── Concurrency ────────────────────────────────────────────────────────

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
            ObjectOptimisticLockingFailureException ex, WebRequest req) {
        log.warn("Optimistic locking failure: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
            "The resource was modified by another request. Please retry.", req);
    }

    // ── Catch-all ──────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, WebRequest req) {
        log.error("Unhandled exception", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
            "An unexpected error occurred. Please try again later.", req);
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status, String errorCode, String message, WebRequest req) {
        var body = ErrorResponse.builder()
            .status(status.value())
            .error(errorCode)
            .message(message)
            .path(req.getDescription(false))
            .timestamp(Instant.now())
            .build();
        return ResponseEntity.status(status).body(body);
    }
}
