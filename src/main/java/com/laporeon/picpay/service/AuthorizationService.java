package com.laporeon.picpay.service;

import com.laporeon.picpay.client.AuthorizationClient;
import com.laporeon.picpay.dtos.response.AuthorizationResponse;
import com.laporeon.picpay.exception.custom.ClientException;
import com.laporeon.picpay.exception.custom.UnauthorizedException;
import feign.FeignException;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {
    private final AuthorizationClient authorizationClient;

    public AuthorizationService(AuthorizationClient authorizationClient) {
        this.authorizationClient = authorizationClient;
    }

    public void ensureAuthorized() {
        AuthorizationResponse response = callAuthorizationService();

        if (response == null || !response.isAuthorized()) {
            throw new UnauthorizedException();
        }
    }

    private AuthorizationResponse callAuthorizationService() {
        try {
            return authorizationClient.authorize();
        } catch (FeignException.Forbidden ex) {
            throw new UnauthorizedException();
        } catch (FeignException ex) {
            throw new ClientException("Failed to call authorization service.");
        }
    }
}
