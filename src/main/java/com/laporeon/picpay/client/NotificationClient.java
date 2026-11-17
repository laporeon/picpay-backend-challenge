package com.laporeon.picpay.client;

import com.laporeon.picpay.dtos.request.NotificationRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "notification-service", url = "${client.notification-service.url}")
public interface NotificationClient {
    @PostMapping("/api/v1/notify")
    ResponseEntity<Void> sendNotification(@RequestBody NotificationRequestDTO dto);
}
