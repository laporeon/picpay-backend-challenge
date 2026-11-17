package com.laporeon.picpay.exception.custom;

import com.laporeon.picpay.enums.WalletType;

public class InvalidDocumentTypeException extends RuntimeException {
    private static final String MESSAGE = "%s accounts must be registered with a %s (%d digits).";

    public InvalidDocumentTypeException(WalletType walletType) {
        super(MESSAGE.formatted(walletType.getLabel(), walletType.getDocumentName(), walletType.getDocumentLength()));
    }
}