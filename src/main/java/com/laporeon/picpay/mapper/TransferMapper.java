package com.laporeon.picpay.mapper;

import com.laporeon.picpay.dtos.response.TransferResponseDTO;
import com.laporeon.picpay.entity.Transfer;
import com.laporeon.picpay.entity.Wallet;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TransferMapper {
    public Transfer toEntity(Wallet payee, Wallet payer, BigDecimal amount) {
        return new Transfer(
                payee,
                payer,
                amount
        );
    }

    public TransferResponseDTO toResponseDTO(Transfer transfer) {
        return new TransferResponseDTO(
                transfer.getAmount(),
                transfer.getPayer().getId(),
                transfer.getPayee().getId()
        );
    }
}
