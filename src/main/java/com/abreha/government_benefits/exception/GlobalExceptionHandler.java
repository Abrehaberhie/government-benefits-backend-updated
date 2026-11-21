package com.abreha.government_benefits.exception;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler
        extends ResponseEntityExceptionHandler {

    private static final Logger LOG =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record FieldError(
            String field,
            String message
    ) {}

    public record ApiError(
            Instant timestamp,
            int status,
            String error,
            String message,
            String path,
            List<FieldError> errors
    ) {}

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        List<FieldError> errors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> new FieldError(
                        error instanceof
                                org.springframework.validation.FieldError field
                                ? field.getField()
                                : "request",
                        error.getDefaultMessage() == null
                                ? "Invalid value"
                                : error.getDefaultMessage()
                ))
                .toList();

        return new ResponseEntity<>(
                createError(status, "Validation failed", request, errors),
                headers,
                status
        );
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            Object body,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        if (request instanceof ServletWebRequest servlet
                && servlet.getResponse() != null
                && servlet.getResponse().isCommitted()) {
            return null;
        }

        String message = switch (status.value()) {
            case 400 ->
                    "Invalid request. Check the JSON body and parameter values.";
            case 404 ->
                    "The requested resource was not found.";
            case 405 ->
                    "This HTTP method is not supported for this endpoint.";
            case 413 ->
                    "The uploaded file exceeds the allowed size.";
            case 415 ->
                    "Unsupported content type.";
            default ->
                    "The request could not be completed.";
        };

        if (status.is5xxServerError()) {
            LOG.error("Request failed", ex);
            message = "An unexpected error occurred. Please try again later.";
        } else if (ex instanceof ResponseStatusException response
                && response.getReason() != null) {
            message = response.getReason();
        }

        return new ResponseEntity<>(
                createError(status, message, request, List.of()),
                headers,
                status
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleConflict(
            DataIntegrityViolationException ex,
            WebRequest request
    ) {
        LOG.warn("Database constraint prevented an operation", ex);

        return new ResponseEntity<>(
                createError(
                        HttpStatus.CONFLICT,
                        "The operation conflicts with an existing or related record.",
                        request,
                        List.of()
                ),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(
            Exception ex,
            WebRequest request
    ) {
        LOG.error("Unexpected request failure", ex);

        return new ResponseEntity<>(
                createError(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "An unexpected error occurred. Please try again later.",
                        request,
                        List.of()
                ),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    private ApiError createError(
            HttpStatusCode status,
            String message,
            WebRequest request,
            List<FieldError> errors
    ) {
        HttpStatus knownStatus = HttpStatus.resolve(status.value());

        String path = request instanceof ServletWebRequest servlet
                ? servlet.getRequest().getRequestURI()
                : "";

        return new ApiError(
                Instant.now(),
                status.value(),
                knownStatus == null
                        ? "Request failed"
                        : knownStatus.getReasonPhrase(),
                message,
                path,
                errors
        );
    }
}
