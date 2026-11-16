package com.laporeon.picpay.service;

import com.laporeon.picpay.dtos.request.TransferRequestDTO;
import com.laporeon.picpay.dtos.response.TransferResponseDTO;
import com.laporeon.picpay.entity.Transfer;
import com.laporeon.picpay.entity.Wallet;
import com.laporeon.picpay.enums.WalletType;
import com.laporeon.picpay.exception.custom.InsufficientBalanceException;
import com.laporeon.picpay.exception.custom.TransferNotAllowedException;
import com.laporeon.picpay.exception.custom.WalletNotFoundException;
import com.laporeon.picpay.mapper.TransferMapper;
import com.laporeon.picpay.repository.TransferRepository;
import com.laporeon.picpay.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TransferService {
    private final TransferRepository transferRepository;
    private final TransferMapper transferMapper;
    private final WalletRepository walletRepository;
    private final AuthorizationService authorizationService;

    public TransferService(TransferRepository transferRepository,
                           TransferMapper transferMapper,
                           WalletRepository walletRepository,
                           AuthorizationService authorizationService) {
        this.transferRepository = transferRepository;
        this.transferMapper = transferMapper;
        this.walletRepository = walletRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public TransferResponseDTO transfer(TransferRequestDTO dto) {
        Wallet payer = findWallet(dto.payerId());
        Wallet payee = findWallet(dto.payeeId());

        validateTransfer(payer, dto);
        authorizationService.ensureAuthorized();

        payer.debit(dto.amount());
        payee.credit(dto.amount());

        Transfer transfer = transferRepository.save(
                transferMapper.toEntity(payee, payer, dto.amount())
        );
        return transferMapper.toResponseDTO(transfer);
    }

    private Wallet findWallet(UUID walletId) {
        return walletRepository.findById(walletId)
                               .orElseThrow(() -> new WalletNotFoundException(walletId));
    }

    private void validateTransfer(Wallet payer, TransferRequestDTO dto) {
        if (payer.getWalletType().equals(WalletType.SHOPKEEPER)) {
            throw new TransferNotAllowedException();
        }

        if (!payer.isBalanceGreaterOrEqual(dto.amount())) {
            throw new InsufficientBalanceException();
        }
    }
}
