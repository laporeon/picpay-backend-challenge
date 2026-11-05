package com.laporeon.picpay.service;

import com.laporeon.picpay.dtos.CreateWalletDTO;
import com.laporeon.picpay.entity.Wallet;
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
    public Wallet createWallet(CreateWalletDTO createWalletDTO) {
        Wallet wallet = new Wallet(
                createWalletDTO.fullName(),
                createWalletDTO.document(),
                createWalletDTO.email(),
                createWalletDTO.password(),
                createWalletDTO.walletType()
        );

        return walletRepository.save(wallet);
    }
}
