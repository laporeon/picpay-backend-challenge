package com.laporeon.picpay.dtos.request;

import com.laporeon.picpay.enums.WalletType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateWalletDTO(
        @NotBlank(message = "Full name is required.")
        @Size(min = 3, max = 255, message = "Name must be between {min} and {max} characters.")
        String fullName,
        @NotBlank(message = "Document is required.")
        @Pattern(
                regexp = "^(\\d{11}|\\d{14})$",
                message = "Document must have 11 digits (CPF) or 14 digits (CNPJ)."
        )
        String document,
        @NotBlank(message = "Email is required")
        @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Invalid email.")
        String email,
        @NotBlank(message = "Password is required.")
        @Pattern(
                regexp = "^(?=\\S{8,25}$)(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).*$",
                message = "Password must be 8-25 characters long and include at least one uppercase letter, one lowercase letter, one number, and one special character."
        )
        String password,
        @NotNull(message = "Wallet type is required.")
        WalletType walletType
) {
}
