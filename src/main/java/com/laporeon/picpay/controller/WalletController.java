package com.laporeon.picpay.controller;

import com.laporeon.picpay.dtos.CreateWalletDTO;
import com.laporeon.picpay.entity.Wallet;
import com.laporeon.picpay.service.WalletService;
import jakarta.validation.Valid;
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
    public ResponseEntity<Wallet> createWallet(@Valid @RequestBody CreateWalletDTO createWalletDTO) {
        var wallet = walletService.createWallet(createWalletDTO);
        return ResponseEntity.ok(wallet);
    }
}
