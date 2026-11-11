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
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class TransferService {
    private final TransferRepository transferRepository;
    private final TransferMapper transferMapper;
    private final WalletRepository walletRepository;

    public TransferService(TransferRepository transferRepository, TransferMapper transferMapper, WalletRepository walletRepository) {
        this.transferRepository = transferRepository;
        this.transferMapper = transferMapper;
        this.walletRepository = walletRepository;
    }

    @Transactional
    public TransferResponseDTO transfer(TransferRequestDTO dto) {
        Wallet payer = walletRepository.findById(dto.payerId())
                                       .orElseThrow(() -> new WalletNotFoundException(dto.payerId()));

        Wallet payee = walletRepository.findById(dto.payeeId())
                                       .orElseThrow(() -> new WalletNotFoundException(dto.payeeId()));

        isTransferAllowed(payer, dto);

        payer.debit(dto.amount());
        payee.credit(dto.amount());

        walletRepository.save(payer);
        walletRepository.save(payee);

        Transfer transfer = transferMapper.toEntity(payee, payer, dto.amount());
        transferRepository.save(transfer);
        return transferMapper.toResponseDTO(transfer);
    }

    private void isTransferAllowed(Wallet payer, TransferRequestDTO dto) {
        if (payer.getWalletType().equals(WalletType.SHOPKEEPER)) {
            throw new TransferNotAllowedException();
        }

        if (!payer.isBalanceGreaterOrEqual(dto.amount())) {
            throw new InsufficientBalanceException();
        }
    }
}
