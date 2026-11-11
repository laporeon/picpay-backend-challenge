package com.laporeon.picpay.exception;

import com.laporeon.picpay.exception.custom.InsufficientBalanceException;
import com.laporeon.picpay.exception.custom.InvalidDocumentTypeException;
import com.laporeon.picpay.exception.custom.RegistrationConflictException;
import com.laporeon.picpay.exception.custom.TransferNotAllowedException;
import com.laporeon.picpay.exception.custom.WalletNotFoundException;
import com.laporeon.picpay.exception.response.ErrorResponse;
import com.laporeon.picpay.exception.response.ValidationErrorResponse;
import com.laporeon.picpay.exception.response.ValidationErrorResponse.FieldError;
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
                "VALIDATION_FAILED",
                "Request validation failed for one or more fields",
                errors,
                Instant.now());

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(error);
    }

    @ExceptionHandler(InvalidDocumentTypeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDocumentTypeException(InvalidDocumentTypeException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNPROCESSABLE_CONTENT.value(),
                "INVALID_DOCUMENT_TYPE",
                ex.getMessage(),
                Instant.now());

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(error);
    }

    @ExceptionHandler(TransferNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleTransferNotAllowedException(TransferNotAllowedException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNPROCESSABLE_CONTENT.value(),
                "TRANSFER_NOT_ALLOWED",
                ex.getMessage(),
                Instant.now());

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(error);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalanceException(InsufficientBalanceException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNPROCESSABLE_CONTENT.value(),
                "INSUFFICIENT_BALANCE",
                ex.getMessage(),
                Instant.now());

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(error);
    }

    @ExceptionHandler(WalletNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWalletNotFoundException(WalletNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "WALLET_NOT_FOUND",
                ex.getMessage(),
                Instant.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(RegistrationConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(RegistrationConflictException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "REGISTRATION_CONFLICT",
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