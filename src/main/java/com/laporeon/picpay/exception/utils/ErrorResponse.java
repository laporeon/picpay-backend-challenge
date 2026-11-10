package com.laporeon.picpay.exception.dtos;

import java.time.Instant;

public record ErrorResponse(
        int status,
        String type,
        String message,
        Instant timestamp
) {
}
