package com.laporeon.picpay.mapper;

import com.laporeon.picpay.dtos.request.CreateWalletDTO;
import com.laporeon.picpay.dtos.response.CreateWalletResponseDTO;
import com.laporeon.picpay.entity.Wallet;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class WalletMapper {
    private final PasswordEncoder passwordEncoder;

    public WalletMapper(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public Wallet toEntity(CreateWalletDTO dto) {
        return new Wallet(
                dto.fullName(),
                dto.document(),
                dto.email(),
                passwordEncoder.encode(dto.password()),
                dto.walletType()
        );
    }

    public CreateWalletResponseDTO toResponseDTO(Wallet wallet) {
        return new CreateWalletResponseDTO(
                wallet.getId(),
                wallet.getFullName(),
                maskDocument(wallet.getDocument()),
                wallet.getEmail(),
                wallet.getWalletType(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt()
        );
    }

    private static String maskDocument(String document) {
        if (document == null || document.isBlank()) {
            return document;
        }

        String digits = document.replaceAll("\\D", "");

        return switch (digits.length()) {
            case 11 -> "***.%s.%s-**".formatted(
                    digits.substring(3, 6),
                    digits.substring(6, 9)
            );
            case 14 -> "**.%s.%s/%s-**".formatted(
                    digits.substring(2, 5),
                    digits.substring(5, 8),
                    digits.substring(8, 12)
            );
            default -> throw new IllegalArgumentException(
                    "Document must have 11 or 14 digits."
            );
        };
    }
}
