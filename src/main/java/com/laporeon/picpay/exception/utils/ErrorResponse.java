package com.laporeon.picpay.exception.utils;

import java.time.Instant;

public record ErrorResponse(
        int status,
        String type,
        String message,
        Instant timestamp
) {
}
