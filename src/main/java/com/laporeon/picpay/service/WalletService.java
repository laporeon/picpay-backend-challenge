package com.laporeon.picpay.service;

import com.laporeon.picpay.dtos.request.CreateWalletDTO;
import com.laporeon.picpay.dtos.response.CreateWalletResponseDTO;
import com.laporeon.picpay.entity.Wallet;
import com.laporeon.picpay.exception.custom.RegistrationConflictException;
import com.laporeon.picpay.mapper.WalletMapper;
import com.laporeon.picpay.repository.WalletRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class WalletService {
    private final WalletRepository walletRepository;
    private final WalletMapper walletMapper;

    public WalletService(WalletRepository walletRepository, WalletMapper walletMapper) {
        this.walletRepository = walletRepository;
        this.walletMapper = walletMapper;
    }

    @Transactional
    public CreateWalletResponseDTO createWallet(CreateWalletDTO dto) {
        if (walletRepository.existsByDocumentOrEmail(dto.document(), dto.email())) {
            throw new RegistrationConflictException("Unable to create wallet due to duplicate document or email.");
        }

        Wallet wallet = walletMapper.toEntity(dto);
        walletRepository.save(wallet);
        return walletMapper.toResponseDTO(wallet);
    }
}
