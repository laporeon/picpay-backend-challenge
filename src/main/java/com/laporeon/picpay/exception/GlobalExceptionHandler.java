package com.laporeon.picpay.exception;

import com.laporeon.picpay.exception.custom.InvalidDocumentTypeException;
import com.laporeon.picpay.exception.custom.RegistrationConflictException;
import com.laporeon.picpay.exception.utils.ErrorResponse;
import com.laporeon.picpay.exception.utils.ValidationErrorResponse;
import com.laporeon.picpay.exception.utils.ValidationErrorResponse.FieldError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        List<FieldError> errors = ex.getBindingResult()
                                    .getFieldErrors()
                                    .stream()
                                    .map(err -> new FieldError(err.getField(), err.getDefaultMessage()))
                                    .toList();

        ValidationErrorResponse error = new ValidationErrorResponse(
                HttpStatus.UNPROCESSABLE_CONTENT.value(),
                "VALIDATION_ERROR",
                "Request validation failed for one or more fields",
                errors,
                Instant.now());

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(error);
    }

    @ExceptionHandler(InvalidDocumentTypeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDocumentTypeException(InvalidDocumentTypeException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "INVALID_DOCUMENT_TYPE",
                ex.getMessage(),
                Instant.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(RegistrationConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(RegistrationConflictException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "CONFLICT_ERROR",
                ex.getMessage(),
                Instant.now());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred",
                Instant.now());

        return ResponseEntity.internalServerError().body(error);
    }
}