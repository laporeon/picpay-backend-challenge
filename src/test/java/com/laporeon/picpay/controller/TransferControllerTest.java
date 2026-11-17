package com.laporeon.picpay.controller;

import com.laporeon.picpay.dtos.request.TransferRequestDTO;
import com.laporeon.picpay.dtos.response.TransferResponseDTO;
import com.laporeon.picpay.exception.custom.ClientException;
import com.laporeon.picpay.exception.custom.InsufficientBalanceException;
import com.laporeon.picpay.exception.custom.TransferNotAllowedException;
import com.laporeon.picpay.exception.custom.UnauthorizedException;
import com.laporeon.picpay.exception.custom.WalletNotFoundException;
import com.laporeon.picpay.service.TransferService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransferController.class)
@DisplayName("TransferController Tests")
class TransferControllerTest {
    private static final String TRANSFERS_URL = "/api/v1/transfers";
    private static final UUID VALID_PAYER_UUID = UUID.fromString("a3b0c02c-1e34-4b7e-9a9f-cf8a7ef77d1e");
    private static final UUID VALID_PAYEE_UUID = UUID.fromString("f97dde36-9fa2-4f7c-8c3d-93efb61fbc79");

    private static final String AMOUNT_MIN_RULE = "Transfer amount must be equal or greater than 3.00.";
    private static final String AMOUNT_DIGITS_RULE = "Transfer amount must have at most 10 integer digits and 2 decimal places";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransferService transferService;

    @Test
    @DisplayName("POST /api/v1/transfers should return 201 with the transfer data")
    void shouldTransferAndReturn201() throws Exception {
        TransferRequestDTO request = validRequest();
        TransferResponseDTO response = new TransferResponseDTO(new BigDecimal("150.00"), VALID_PAYER_UUID, VALID_PAYEE_UUID);
        when(transferService.transfer(request)).thenReturn(response);

        mockMvc.perform(post(TRANSFERS_URL)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.value").value(150.00))
               .andExpect(jsonPath("$.payer").value(VALID_PAYER_UUID.toString()))
               .andExpect(jsonPath("$.payee").value(VALID_PAYEE_UUID.toString()));

        verify(transferService).transfer(request);
    }

    @Test
    @DisplayName("POST /api/v1/transfers should return 201 when amount is exactly the minimum allowed (3.00)")
    void shouldTransferAndReturn201WhenAmountIsMinimum() throws Exception {
        TransferRequestDTO request = validRequestWith("amount", new BigDecimal("3.00"));
        TransferResponseDTO response = new TransferResponseDTO(new BigDecimal("3.00"), VALID_PAYER_UUID, VALID_PAYEE_UUID);
        when(transferService.transfer(request)).thenReturn(response);

        mockMvc.perform(post(TRANSFERS_URL)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.value").value(3.00))
               .andExpect(jsonPath("$.payer").value(VALID_PAYER_UUID.toString()))
               .andExpect(jsonPath("$.payee").value(VALID_PAYEE_UUID.toString()));

        verify(transferService).transfer(request);
    }

    @Test
    @DisplayName("POST /api/v1/transfers should return 422 with every required field error when body is empty")
    void shouldReturn422WhenAllFieldsAreMissing() throws Exception {
        mockMvc.perform(post(TRANSFERS_URL)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content("{}"))
               .andExpect(status().isUnprocessableContent())
               .andExpect(jsonPath("$.status").value(422))
               .andExpect(jsonPath("$.type").value("VALIDATION_FAILED"))
               .andExpect(jsonPath("$.message").value("Request validation failed for one or more fields"))
               .andExpect(jsonPath("$.timestamp").exists())
               .andExpect(jsonPath("$.errors", hasSize(3)))
               .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("amount", "payerId", "payeeId")))
               .andExpect(jsonPath("$.errors[*].message").value(containsInAnyOrder(
                       "Transfer amount is required.",
                       "Payer id is required.",
                       "Payee id is required.")));

        verify(transferService, never()).transfer(any());
    }

    static Stream<Arguments> invalidFields() {
        return Stream.of(
                arguments("amount null",              "amount",  null,                              "Transfer amount is required."),
                arguments("amount 2.99",              "amount",  new BigDecimal("2.99"),            AMOUNT_MIN_RULE),
                arguments("amount zero",              "amount",  new BigDecimal("0.00"),            AMOUNT_MIN_RULE),
                arguments("amount negative",          "amount",  new BigDecimal("-10.00"),          AMOUNT_MIN_RULE),
                arguments("amount 4 decimal places",  "amount",  new BigDecimal("100.1234"),        AMOUNT_DIGITS_RULE),
                arguments("amount 11 integer digits", "amount",  new BigDecimal("12345678901.00"),  AMOUNT_DIGITS_RULE),
                arguments("payerId null",             "payerId", null,                              "Payer id is required."),
                arguments("payeeId null",             "payeeId", null,                              "Payee id is required.")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidFields")
    @DisplayName("POST /api/v1/transfers should return 422 with a single field error")
    void shouldReturn422ForInvalidField(String scenario, String field, Object invalidValue,
                                        String message) throws Exception {
        mockMvc.perform(post(TRANSFERS_URL)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(validRequestWith(field, invalidValue))))
               .andExpect(status().isUnprocessableContent())
               .andExpect(jsonPath("$.type").value("VALIDATION_FAILED"))
               .andExpect(jsonPath("$.errors", hasSize(1)))
               .andExpect(jsonPath("$.errors[0].field").value(field))
               .andExpect(jsonPath("$.errors[0].message").value(message));

        verify(transferService, never()).transfer(any());
    }

    @Test
    @DisplayName("POST /api/v1/transfers should return 404 when a wallet is not found")
    void shouldReturn404WhenServiceThrowsWalletNotFound() throws Exception {
        UUID missingUuid = UUID.fromString("f97dde36-9fa2-4f7c-8c3d-94efb61fbc59");
        TransferRequestDTO request = validRequestWith("payeeId", missingUuid);
        when(transferService.transfer(any(TransferRequestDTO.class)))
                .thenThrow(new WalletNotFoundException(missingUuid));

        mockMvc.perform(post(TRANSFERS_URL)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isNotFound())
               .andExpect(jsonPath("$.status").value(404))
               .andExpect(jsonPath("$.type").value("WALLET_NOT_FOUND"))
               .andExpect(jsonPath("$.message").value("No wallet found for id '" + missingUuid + "'."))
               .andExpect(jsonPath("$.timestamp").exists());

        verify(transferService).transfer(request);
    }

    @Test
    @DisplayName("POST /api/v1/transfers should return 422 when payer is a shopkeeper")
    void shouldReturn422WhenServiceThrowsTransferNotAllowed() throws Exception {
        TransferRequestDTO request = validRequest();
        when(transferService.transfer(any(TransferRequestDTO.class)))
                .thenThrow(new TransferNotAllowedException());

        mockMvc.perform(post(TRANSFERS_URL)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isUnprocessableContent())
               .andExpect(jsonPath("$.status").value(422))
               .andExpect(jsonPath("$.type").value("TRANSFER_NOT_ALLOWED"))
               .andExpect(jsonPath("$.message").value("Shopkeeper accounts are not allowed to transfer."))
               .andExpect(jsonPath("$.timestamp").exists());

        verify(transferService).transfer(request);
    }

    @Test
    @DisplayName("POST /api/v1/transfers should return 422 when payer has insufficient balance")
    void shouldReturn422WhenServiceThrowsInsufficientBalance() throws Exception {
        TransferRequestDTO request = validRequest();
        when(transferService.transfer(any(TransferRequestDTO.class)))
                .thenThrow(new InsufficientBalanceException());

        mockMvc.perform(post(TRANSFERS_URL)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isUnprocessableContent())
               .andExpect(jsonPath("$.status").value(422))
               .andExpect(jsonPath("$.type").value("INSUFFICIENT_BALANCE"))
               .andExpect(jsonPath("$.message").value("Unable to complete transfer due to insufficient balance."))
               .andExpect(jsonPath("$.timestamp").exists());

        verify(transferService).transfer(request);
    }

    @Test
    @DisplayName("POST /api/v1/transfers should return 403 when transfer is rejected by authorization service")
    void shouldReturn403WhenServiceThrowsUnauthorized() throws Exception {
        TransferRequestDTO request = validRequest();
        when(transferService.transfer(any(TransferRequestDTO.class)))
                .thenThrow(new UnauthorizedException());

        mockMvc.perform(post(TRANSFERS_URL)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isForbidden())
               .andExpect(jsonPath("$.status").value(403))
               .andExpect(jsonPath("$.type").value("TRANSFER_NOT_AUTHORIZED"))
               .andExpect(jsonPath("$.message").value("Transfer rejected by authorization service."))
               .andExpect(jsonPath("$.timestamp").exists());

        verify(transferService).transfer(request);
    }

    @Test
    @DisplayName("POST /api/v1/transfers should return 502 when authorization service call fails")
    void shouldReturn502WhenServiceThrowsClientException() throws Exception {
        TransferRequestDTO request = validRequest();
        when(transferService.transfer(any(TransferRequestDTO.class)))
                .thenThrow(new ClientException("Failed to call authorization service."));

        mockMvc.perform(post(TRANSFERS_URL)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isBadGateway())
               .andExpect(jsonPath("$.status").value(502))
               .andExpect(jsonPath("$.type").value("CLIENT_SERVICE_ERROR"))
               .andExpect(jsonPath("$.message").value("Failed to call authorization service."))
               .andExpect(jsonPath("$.timestamp").exists());

        verify(transferService).transfer(request);
    }

    @Test
    @DisplayName("POST /api/v1/transfers should return 500 without leaking internal details when an unexpected error occurs")
    void shouldReturn500WhenServiceThrowsUnexpectedException() throws Exception {
        TransferRequestDTO request = validRequest();
        when(transferService.transfer(any(TransferRequestDTO.class)))
                .thenThrow(new RuntimeException("Database is down"));

        mockMvc.perform(post(TRANSFERS_URL)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isInternalServerError())
               .andExpect(jsonPath("$.status").value(500))
               .andExpect(jsonPath("$.type").value("INTERNAL_SERVER_ERROR"))
               .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
               .andExpect(jsonPath("$.timestamp").exists());

        verify(transferService).transfer(request);
    }

    private static TransferRequestDTO validRequest() {
        return new TransferRequestDTO(new BigDecimal("150.00"), VALID_PAYER_UUID, VALID_PAYEE_UUID);
    }

    private static TransferRequestDTO validRequestWith(String field, Object value) {
        TransferRequestDTO v = validRequest();
        return switch (field) {
            case "amount"  -> new TransferRequestDTO((BigDecimal) value, v.payerId(), v.payeeId());
            case "payerId" -> new TransferRequestDTO(v.amount(), (UUID) value, v.payeeId());
            case "payeeId" -> new TransferRequestDTO(v.amount(), v.payerId(), (UUID) value);
            default -> throw new IllegalArgumentException("Unknown field: " + field);
        };
    }
}