package com.laporeon.picpay.exception.custom;

public class TransferNotAllowedException extends RuntimeException {
  private static final String MESSAGE = "Shopkeeper accounts are not allowed to transfer.";

  public TransferNotAllowedException() {
        super(MESSAGE);
    }
}
