package com.laporeon.picpay.service;

import com.laporeon.picpay.client.AuthorizationClient;
import com.laporeon.picpay.dtos.response.AuthorizationResponse;
import com.laporeon.picpay.exception.custom.ClientException;
import com.laporeon.picpay.exception.custom.UnauthorizedException;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorizationService Tests")
class AuthorizationServiceTest {
    private static final String AUTHORIZE_URL = "https://util.devi.tools/api/v2/authorize";
    private static final String UNAUTHORIZED_MESSAGE = "Transfer rejected by authorization service.";
    private static final String CLIENT_ERROR_MESSAGE = "Failed to call authorization service.";

    @Mock
    private AuthorizationClient authorizationClient;

    @InjectMocks
    private AuthorizationService authorizationService;

    static Stream<Arguments> authorizedResponses() {
        return Stream.of(
                arguments("status success",           new AuthorizationResponse("success", new AuthorizationResponse.Data(true))),
                arguments("status SUCCESS uppercase", new AuthorizationResponse("SUCCESS", new AuthorizationResponse.Data(true)))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("authorizedResponses")
    @DisplayName("Should not throw when authorization service authorizes the transfer")
    void shouldAuthorizeSuccessfully(String scenario, AuthorizationResponse response) {
        when(authorizationClient.authorize()).thenReturn(response);

        assertDoesNotThrow(() -> authorizationService.ensureAuthorized());

        verify(authorizationClient).authorize();
    }

    static Stream<Arguments> unauthorizedResponses() {
        return Stream.of(
                arguments("null response",                  null),
                arguments("status fail",                    new AuthorizationResponse("fail", new AuthorizationResponse.Data(false))),
                arguments("status fail with authorization", new AuthorizationResponse("fail", new AuthorizationResponse.Data(true))),
                arguments("status success without data",   new AuthorizationResponse("success", null)),
                arguments("status success denied",         new AuthorizationResponse("success", new AuthorizationResponse.Data(false))),
                arguments("status null",                    new AuthorizationResponse(null, new AuthorizationResponse.Data(true)))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unauthorizedResponses")
    @DisplayName("Should throw UnauthorizedException when authorization service does not authorize the transfer")
    void shouldThrowUnauthorizedWhenResponseIsNotAuthorized(String scenario, AuthorizationResponse response) {
        when(authorizationClient.authorize()).thenReturn(response);

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authorizationService.ensureAuthorized()
        );

        assertEquals(UNAUTHORIZED_MESSAGE, exception.getMessage());
        verify(authorizationClient).authorize();
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when authorization service responds with 403")
    void shouldThrowUnauthorizedWhenClientReturnsForbidden() {
        when(authorizationClient.authorize()).thenThrow(feignException(403));

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authorizationService.ensureAuthorized()
        );

        assertEquals(UNAUTHORIZED_MESSAGE, exception.getMessage());
        verify(authorizationClient).authorize();
    }

    @ParameterizedTest(name = "HTTP {0}")
    @ValueSource(ints = {400, 401, 404, 429, 500, 502, 503, 504})
    @DisplayName("Should throw ClientException when authorization service fails with a non-403 status")
    void shouldThrowClientExceptionWhenClientFails(int status) {
        when(authorizationClient.authorize()).thenThrow(feignException(status));

        ClientException exception = assertThrows(
                ClientException.class,
                () -> authorizationService.ensureAuthorized()
        );

        assertEquals(CLIENT_ERROR_MESSAGE, exception.getMessage());
        verify(authorizationClient).authorize();
    }

    private static FeignException feignException(int status) {
        Request request = Request.create(
                Request.HttpMethod.GET, AUTHORIZE_URL, Map.of(), null, StandardCharsets.UTF_8, null
        );
        Response response = Response.builder()
                                    .status(status)
                                    .reason("HTTP " + status)
                                    .request(request)
                                    .headers(Map.of())
                                    .build();
        return FeignException.errorStatus("AuthorizationClient#authorize()", response);
    }
}
