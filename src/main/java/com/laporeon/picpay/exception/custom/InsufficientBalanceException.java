package com.laporeon.picpay.exception.custom;

public class InsufficientBalanceException extends RuntimeException {
  private static final String MESSAGE = "Unable to complete transfer due to insufficient balance.";

  public InsufficientBalanceException() {
        super(MESSAGE);
    }
}
