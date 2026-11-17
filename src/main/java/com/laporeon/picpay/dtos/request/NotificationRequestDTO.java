package com.laporeon.picpay.dtos.request;

import java.math.BigDecimal;

public record NotificationRequestDTO(
        String payerName,
        String payeeName,
        BigDecimal amount
) {
}
