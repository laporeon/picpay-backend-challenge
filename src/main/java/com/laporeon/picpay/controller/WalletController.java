package com.laporeon.picpay.controller;

import com.laporeon.picpay.dtos.request.CreateWalletDTO;
import com.laporeon.picpay.dtos.response.CreateWalletResponseDTO;
import com.laporeon.picpay.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {
    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    public ResponseEntity<CreateWalletResponseDTO> createWallet(@Valid @RequestBody CreateWalletDTO dto) {
        CreateWalletResponseDTO wallet = walletService.createWallet(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(wallet);
    }
}
