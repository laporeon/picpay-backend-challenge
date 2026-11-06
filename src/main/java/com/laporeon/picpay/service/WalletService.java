package com.laporeon.picpay.service;

import com.laporeon.picpay.dtos.CreateWalletDTO;
import com.laporeon.picpay.entity.Wallet;
import com.laporeon.picpay.exception.custom.RegistrationConflictException;
import com.laporeon.picpay.repository.WalletRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class WalletService {
    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Transactional
    public Wallet createWallet(CreateWalletDTO dto) {
        if (walletRepository.existsByDocumentOrEmail(dto.document(), dto.email())) {
            throw new RegistrationConflictException("Unable to create wallet due to duplicate document or email.");
        }

        Wallet wallet = new Wallet(
                dto.fullName(),
                dto.document(),
                dto.email(),
                dto.password(),
                dto.walletType()
        );

        return walletRepository.save(wallet);
    }
}
