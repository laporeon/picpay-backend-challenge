package com.laporeon.picpay.controller;

import com.laporeon.picpay.dtos.request.TransferRequestDTO;
import com.laporeon.picpay.dtos.response.TransferResponseDTO;
import com.laporeon.picpay.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {
    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<TransferResponseDTO> transfer(@Valid @RequestBody TransferRequestDTO dto) {
        TransferResponseDTO transfer = transferService.transfer(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(transfer);
    }
}
