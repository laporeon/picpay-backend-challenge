package com.laporeon.picpay.service;

import com.laporeon.picpay.client.NotificationClient;
import com.laporeon.picpay.dtos.request.NotificationRequestDTO;
import feign.FeignException;
import feign.Request;
import feign.RetryableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import feign.Response;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
@DisplayName("NotificationService Tests")
class NotificationServiceTest {
    private static final String NOTIFY_URL = "https://util.devi.tools/api/v1/notify";
    private static final String SENT_MESSAGE = "notification sent: Acme Store, you received 150.00 from John Doe.";
    private static final String UNEXPECTED_STATUS_MESSAGE = "notification service returned an unexpected status";
    private static final String FAILURE_MESSAGE = "failed to send notification";

    private static final NotificationRequestDTO NOTIFICATION =
            new NotificationRequestDTO("John Doe", "Acme Store", new BigDecimal("150.00"));

    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("Should send notification and log success when notification service responds with 204")
    void shouldSendNotificationSuccessfully(CapturedOutput output) {
        when(notificationClient.sendNotification(NOTIFICATION)).thenReturn(ResponseEntity.noContent().build());

        assertDoesNotThrow(() -> notificationService.sendNotification(NOTIFICATION));

        assertTrue(output.getOut().contains(SENT_MESSAGE));
        assertFalse(output.getOut().contains(UNEXPECTED_STATUS_MESSAGE));
        assertFalse(output.getOut().contains(FAILURE_MESSAGE));
        verify(notificationClient).sendNotification(NOTIFICATION);
        verifyNoMoreInteractions(notificationClient);
    }

    @ParameterizedTest(name = "HTTP {0}")
    @ValueSource(ints = {200, 201, 202, 206})
    @DisplayName("Should not throw and log a warning when notification service responds with a non-204 success status")
    void shouldLogWarningWhenStatusIsUnexpected(int status, CapturedOutput output) {
        when(notificationClient.sendNotification(NOTIFICATION)).thenReturn(ResponseEntity.status(status).build());

        assertDoesNotThrow(() -> notificationService.sendNotification(NOTIFICATION));

        assertTrue(output.getOut().contains(UNEXPECTED_STATUS_MESSAGE + " " + status));
        assertFalse(output.getOut().contains(SENT_MESSAGE));
        verify(notificationClient).sendNotification(NOTIFICATION);
        verifyNoMoreInteractions(notificationClient);
    }

    @ParameterizedTest(name = "HTTP {0}")
    @ValueSource(ints = {400, 401, 403, 404, 429, 500, 502, 503, 504})
    @DisplayName("Should swallow FeignException and log a warning when notification service fails")
    void shouldNotThrowWhenClientFails(int status, CapturedOutput output) {
        when(notificationClient.sendNotification(NOTIFICATION)).thenThrow(feignException(status));

        assertDoesNotThrow(() -> notificationService.sendNotification(NOTIFICATION));

        assertTrue(output.getOut().contains(FAILURE_MESSAGE));
        assertFalse(output.getOut().contains(SENT_MESSAGE));
        verify(notificationClient).sendNotification(NOTIFICATION);
        verifyNoMoreInteractions(notificationClient);
    }

    @Test
    @DisplayName("Should swallow RetryableException and log a warning when notification service is unreachable")
    void shouldNotThrowWhenClientIsUnreachable(CapturedOutput output) {
        RetryableException timeout = new RetryableException(
                -1, "Read timed out", Request.HttpMethod.POST, (Long) null, request()
        );
        when(notificationClient.sendNotification(NOTIFICATION)).thenThrow(timeout);

        assertDoesNotThrow(() -> notificationService.sendNotification(NOTIFICATION));

        assertTrue(output.getOut().contains(FAILURE_MESSAGE));
        assertFalse(output.getOut().contains(SENT_MESSAGE));
        verify(notificationClient).sendNotification(NOTIFICATION);
        verifyNoMoreInteractions(notificationClient);
    }

    private static Request request() {
        return Request.create(
                Request.HttpMethod.POST, NOTIFY_URL, Map.of(), null, StandardCharsets.UTF_8, null
        );
    }

    private static FeignException feignException(int status) {
        Response response = Response.builder()
                                    .status(status)
                                    .reason("HTTP " + status)
                                    .request(request())
                                    .headers(Map.of())
                                    .build();
        return FeignException.errorStatus("NotificationClient#sendNotification(NotificationRequestDTO)", response);
    }
}
