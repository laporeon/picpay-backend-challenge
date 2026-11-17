package com.laporeon.picpay.service;

import com.laporeon.picpay.client.NotificationClient;
import com.laporeon.picpay.dtos.request.NotificationRequestDTO;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.text.NumberFormat;
import java.util.Locale;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final String NOTIFICATION_MESSAGE = "%s, you received %s from %s.";

    private final NotificationClient notificationClient;

    public NotificationService(NotificationClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendNotification(NotificationRequestDTO dto) {
        String amount = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(dto.amount());
        String message = NOTIFICATION_MESSAGE.formatted(dto.payeeName(), amount, dto.payerName());

        try {
            ResponseEntity<Void> response = notificationClient.sendNotification(dto);

            if (!response.getStatusCode().isSameCodeAs(HttpStatus.NO_CONTENT)) {
                log.warn("notification service returned an unexpected status {}", response.getStatusCode().value());
                return;
            }

            log.info("notification sent: {}", message);
        } catch (FeignException ex) {
            log.warn("failed to send notification", ex);
        }
    }
}
