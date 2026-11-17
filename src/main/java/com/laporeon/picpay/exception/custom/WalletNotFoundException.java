package com.laporeon.picpay.exception.custom;

import java.util.UUID;

public class WalletNotFoundException extends RuntimeException {
    public WalletNotFoundException(UUID id) {
        super("No wallet found for id '" + id + "'.");
    }
}
