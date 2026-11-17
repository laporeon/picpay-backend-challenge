package com.laporeon.picpay.controller;

import com.laporeon.picpay.dtos.request.CreateWalletDTO;
import com.laporeon.picpay.dtos.response.CreateWalletResponseDTO;
import com.laporeon.picpay.enums.WalletType;
import com.laporeon.picpay.exception.custom.InvalidDocumentTypeException;
import com.laporeon.picpay.exception.custom.RegistrationConflictException;
import com.laporeon.picpay.service.WalletService;
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

import java.time.Instant;
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

@WebMvcTest(WalletController.class)
@DisplayName("WalletController Tests")
class WalletControllerTest {
    private static final String WALLETS_URL = "/api/v1/wallets";
    private static final UUID WALLET_ID = UUID.fromString("3f1c9a2e-8b4d-4c1a-9f2e-1a2b3c4d5e6f");
    private static final Instant FIXED_NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final String FIXED_NOW_JSON = "2026-01-01T10:00:00Z";

    private static final String PASSWORD_RULE = "Password must be 8-25 characters long and include at least one " +
            "uppercase letter, one lowercase letter, one number, and one special character.";
    private static final String DOCUMENT_RULE = "Document must have 11 digits (CPF) or 14 digits (CNPJ).";
    private static final String NAME_RULE = "Name must be between 3 and 255 characters.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WalletService walletService;

    @Test
    @DisplayName("POST /api/v1/wallets should return 201 with the created COMMON wallet")
    void shouldCreateCommonWalletAndReturn201() throws Exception {
        CreateWalletDTO request = validRequest();
        CreateWalletResponseDTO response = new CreateWalletResponseDTO(
                WALLET_ID,
                "John Doe",
                "***.982.247-**",
                "john.doe@email.com",
                WalletType.COMMON,
                FIXED_NOW,
                FIXED_NOW
        );
        when(walletService.createWallet(request)).thenReturn(response);

        mockMvc.perform(post(WALLETS_URL)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(WALLET_ID.toString()))
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andExpect(jsonPath("$.document").value("***.982.247-**"))
                .andExpect(jsonPath("$.email").value("john.doe@email.com"))
                .andExpect(jsonPath("$.walletType").value("COMMON"))
                .andExpect(jsonPath("$.createdAt").value(FIXED_NOW_JSON))
                .andExpect(jsonPath("$.updatedAt").value(FIXED_NOW_JSON))
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(walletService).createWallet(request);
    }

    @Test
    @DisplayName("POST /api/v1/wallets should return 201 with the created SHOPKEEPER wallet")
    void shouldCreateShopkeeperWalletAndReturn201() throws Exception {
        CreateWalletDTO request = new CreateWalletDTO(
                "Acme Store",
                "11222333000181",
                "contact@acme.com",
                "Str0ng@Pass",
                WalletType.SHOPKEEPER
        );
        CreateWalletResponseDTO response = new CreateWalletResponseDTO(
                WALLET_ID,
                "Acme Store",
                "**.222.333/0001-**",
                "contact@acme.com",
                WalletType.SHOPKEEPER,
                FIXED_NOW,
                FIXED_NOW
        );
        when(walletService.createWallet(request)).thenReturn(response);

        mockMvc.perform(post(WALLETS_URL)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(WALLET_ID.toString()))
                .andExpect(jsonPath("$.fullName").value("Acme Store"))
                .andExpect(jsonPath("$.document").value("**.222.333/0001-**"))
                .andExpect(jsonPath("$.email").value("contact@acme.com"))
                .andExpect(jsonPath("$.walletType").value("SHOPKEEPER"))
                .andExpect(jsonPath("$.createdAt").value(FIXED_NOW_JSON))
                .andExpect(jsonPath("$.updatedAt").value(FIXED_NOW_JSON))
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(walletService).createWallet(request);
    }

    @Test
    @DisplayName("POST /api/v1/wallets should return 422 with every required field error when body is empty")
    void shouldReturn422WhenAllFieldsAreMissing() throws Exception {
        mockMvc.perform(post(WALLETS_URL)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content("{}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Request validation failed for one or more fields"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors", hasSize(5)))
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder(
                        "fullName", "document", "email", "password", "walletType")))
                .andExpect(jsonPath("$.errors[*].message").value(containsInAnyOrder(
                        "Full name is required.",
                        "Document is required.",
                        "Email is required.",
                        "Password is required.",
                        "Wallet type is required.")));

        verify(walletService, never()).createWallet(any());
    }

    static Stream<Arguments> invalidFields() {
        return Stream.of(
                arguments("fullName null",        "fullName",   null,                           "Full name is required."),
                arguments("fullName blank",       "fullName",   "   ",                          "Full name is required."),
                arguments("fullName 2 chars",     "fullName",   "Jo",                           NAME_RULE),
                arguments("fullName 256 chars",   "fullName",   "a".repeat(256),          NAME_RULE),
                arguments("document null",        "document",   null,                           "Document is required."),
                arguments("document 12 digits",   "document",   "123456789012",                 DOCUMENT_RULE),
                arguments("document non-digits",  "document",   "1ABC4567890",                  DOCUMENT_RULE),
                arguments("email null",           "email",      null,                           "Email is required."),
                arguments("email invalid format", "email",      "invalid-email.com",            "Invalid email."),
                arguments("password null",        "password",   null,                           "Password is required."),
                arguments("password 7 chars",     "password",   "Str0ng@",                      PASSWORD_RULE),
                arguments("password 26 chars",    "password",   "Str0ng@Pass" + "x".repeat(15), PASSWORD_RULE),
                arguments("password weak",        "password",   "weakpassword",                 PASSWORD_RULE),
                arguments("walletType null",      "walletType", null,                           "Wallet type is required.")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidFields")
    @DisplayName("POST /api/v1/wallets should return 422 with a single field error")
    void shouldReturn422ForInvalidField(String scenario, String field, Object invalidValue,
                                        String message) throws Exception {
        mockMvc.perform(post(WALLETS_URL)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(validRequestWith(field, invalidValue))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.type").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field").value(field))
                .andExpect(jsonPath("$.errors[0].message").value(message));

        verify(walletService, never()).createWallet(any());
    }

    @Test
    @DisplayName("POST /api/v1/wallets should return 422 when service rejects document for the wallet type")
    void shouldReturn422WhenServiceThrowsInvalidDocumentType() throws Exception {
        CreateWalletDTO request = new CreateWalletDTO(
                "Acme Store",
                "52998224725", // CPF on a SHOPKEEPER wallet
                "contact@acme.com",
                "Str0ng@Pass",
                WalletType.SHOPKEEPER
        );
        when(walletService.createWallet(any(CreateWalletDTO.class)))
                .thenThrow(new InvalidDocumentTypeException(WalletType.SHOPKEEPER));

        mockMvc.perform(post(WALLETS_URL)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("INVALID_DOCUMENT_TYPE"))
                .andExpect(jsonPath("$.message").value("Shopkeeper accounts must be registered with a CNPJ (14 digits)."))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(walletService).createWallet(request);
    }

    @Test
    @DisplayName("POST /api/v1/wallets should return 409 when document or email is already registered")
    void shouldReturn409WhenServiceThrowsRegistrationConflict() throws Exception {
        CreateWalletDTO request = validRequest();
        when(walletService.createWallet(any(CreateWalletDTO.class)))
                .thenThrow(new RegistrationConflictException("Unable to create wallet due to duplicate document or email."));

        mockMvc.perform(post(WALLETS_URL)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.type").value("REGISTRATION_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Unable to create wallet due to duplicate document or email."))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(walletService).createWallet(request);
    }

    @Test
    @DisplayName("POST /api/v1/wallets should return 500 without leaking internal details when an unexpected error occurs")
    void shouldReturn500WhenServiceThrowsUnexpectedException() throws Exception {
        CreateWalletDTO request = validRequest();
        when(walletService.createWallet(any(CreateWalletDTO.class)))
                .thenThrow(new RuntimeException("Database is down"));

        mockMvc.perform(post(WALLETS_URL)
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.type").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(walletService).createWallet(request);
    }

    private static CreateWalletDTO validRequest() {
        return new CreateWalletDTO(
                "John Doe",
                "52998224725",
                "john.doe@email.com",
                "Str0ng@Pass",
                WalletType.COMMON
        );
    }

    private static CreateWalletDTO validRequestWith(String field, Object value) {
        CreateWalletDTO v = validRequest();
        return switch (field) {
            case "fullName"   -> new CreateWalletDTO((String) value, v.document(), v.email(), v.password(), v.walletType());
            case "document"   -> new CreateWalletDTO(v.fullName(), (String) value, v.email(), v.password(), v.walletType());
            case "email"      -> new CreateWalletDTO(v.fullName(), v.document(), (String) value, v.password(), v.walletType());
            case "password"   -> new CreateWalletDTO(v.fullName(), v.document(), v.email(), (String) value, v.walletType());
            case "walletType" -> new CreateWalletDTO(v.fullName(), v.document(), v.email(), v.password(), (WalletType) value);
            default -> throw new IllegalArgumentException("Unknown field: " + field);
        };
    }
}