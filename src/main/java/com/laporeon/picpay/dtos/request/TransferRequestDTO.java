package com.laporeon.picpay.dtos.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequestDTO(
        @NotNull(message = "Transfer amount is required.")
        @DecimalMin(value = "3.00", message = "Transfer amount must be equal or greater than {value}.")
        @Digits(integer = 10, fraction = 2, message = "Transfer amount must have at most 10 integer digits and 2 decimal places")
        BigDecimal amount,
        @NotNull(message = "Payer id is required.")
        UUID payerId,
        @NotNull(message = "Payee id is required.")
        UUID payeeId
) {
}
