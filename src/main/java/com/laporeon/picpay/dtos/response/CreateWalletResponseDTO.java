package com.laporeon.picpay.dtos.response;

import com.laporeon.picpay.enums.WalletType;

import java.time.Instant;
import java.util.UUID;

public record CreateWalletResponseDTO(
        UUID id,
        String fullName,
        String document,
        String email,
        WalletType walletType,
        Instant createdAt,
        Instant updatedAt
) {
}
