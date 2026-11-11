package com.laporeon.picpay.exception.response;

import java.time.Instant;

public record ErrorResponse(
        int status,
        String type,
        String message,
        Instant timestamp
) {
}
