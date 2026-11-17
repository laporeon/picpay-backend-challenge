package com.laporeon.picpay.exception.custom;

public class RegistrationConflictException extends RuntimeException {
    public RegistrationConflictException(String message) {
        super(message);
    }
}
