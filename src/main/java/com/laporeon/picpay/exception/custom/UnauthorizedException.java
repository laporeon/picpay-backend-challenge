package com.laporeon.picpay.exception.custom;

public class UnauthorizedException extends RuntimeException {
  private static final String MESSAGE = "Transfer rejected by authorization service.";

  public UnauthorizedException() {
        super(MESSAGE);
    }
}
