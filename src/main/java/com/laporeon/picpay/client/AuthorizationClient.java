package com.laporeon.picpay.client;

import com.laporeon.picpay.dtos.response.AuthorizationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(value = "authorization-service", url = "${client.authorization-service.url}")
public interface AuthorizationClient {
    @GetMapping("/api/v2/authorize")
    AuthorizationResponse authorize();
}
