package com.laporeon.picpay.dtos.response;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferResponseDTO(
        BigDecimal value,
        UUID payer,
        UUID payee
) {
}
