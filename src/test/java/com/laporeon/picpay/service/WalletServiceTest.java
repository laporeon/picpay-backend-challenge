package com.laporeon.picpay.service;

import com.laporeon.picpay.dtos.request.CreateWalletDTO;
import com.laporeon.picpay.dtos.response.CreateWalletResponseDTO;
import com.laporeon.picpay.entity.Wallet;
import com.laporeon.picpay.enums.WalletType;
import com.laporeon.picpay.exception.custom.InvalidDocumentTypeException;
import com.laporeon.picpay.exception.custom.RegistrationConflictException;
import com.laporeon.picpay.mapper.WalletMapper;
import com.laporeon.picpay.repository.WalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WalletService Tests")
class WalletServiceTest {
    private static final UUID WALLET_ID = UUID.fromString("3f1c9a2e-8b4d-4c1a-9f2e-1a2b3c4d5e6f");
    private static final Instant FIXED_NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final String CPF = "52998224725";
    private static final String CNPJ = "11222333000181";
    private static final String PASSWORD = "Str0ng@Pass";

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletMapper walletMapper;

    @InjectMocks
    private WalletService walletService;

    static Stream<Arguments> validRequests() {
        return Stream.of(
                arguments("COMMON with CPF",
                        new CreateWalletDTO("John Doe", CPF, "john.doe@email.com", PASSWORD, WalletType.COMMON)),
                arguments("SHOPKEEPER with CNPJ",
                        new CreateWalletDTO("Acme Store", CNPJ, "contact@acme.com", PASSWORD, WalletType.SHOPKEEPER))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validRequests")
    @DisplayName("Should create wallet when document matches wallet type and is not registered")
    void shouldCreateWalletSuccessfully(String scenario, CreateWalletDTO dto) {
        Wallet wallet = new Wallet(dto.fullName(), dto.document(), dto.email(), "hashed-password", dto.walletType());
        CreateWalletResponseDTO expectedResponse = new CreateWalletResponseDTO(
                WALLET_ID, dto.fullName(), "masked-document", dto.email(), dto.walletType(), FIXED_NOW, FIXED_NOW
        );

        when(walletRepository.existsByDocumentOrEmail(dto.document(), dto.email())).thenReturn(false);
        when(walletMapper.toEntity(dto)).thenReturn(wallet);
        when(walletRepository.save(wallet)).thenReturn(wallet);
        when(walletMapper.toResponseDTO(wallet)).thenReturn(expectedResponse);

        CreateWalletResponseDTO response = walletService.createWallet(dto);

        assertSame(expectedResponse, response);

        InOrder order = inOrder(walletRepository, walletMapper);
        order.verify(walletRepository).existsByDocumentOrEmail(dto.document(), dto.email());
        order.verify(walletMapper).toEntity(dto);
        order.verify(walletRepository).save(wallet);
        order.verify(walletMapper).toResponseDTO(wallet);
    }

    static Stream<Arguments> documentTypeMismatches() {
        return Stream.of(
                arguments("COMMON with CNPJ", WalletType.COMMON, CNPJ,
                        "Common users accounts must be registered with a CPF (11 digits)."),
                arguments("SHOPKEEPER with CPF", WalletType.SHOPKEEPER, CPF,
                        "Shopkeeper accounts must be registered with a CNPJ (14 digits).")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("documentTypeMismatches")
    @DisplayName("Should throw InvalidDocumentTypeException when document does not match wallet type")
    void shouldThrowInvalidDocumentTypeWhenDocumentDoesNotMatchWalletType(String scenario, WalletType walletType,
                                                                          String document, String message) {
        CreateWalletDTO dto = new CreateWalletDTO("John Doe", document, "john.doe@email.com", PASSWORD, walletType);

        InvalidDocumentTypeException exception = assertThrows(
                InvalidDocumentTypeException.class,
                () -> walletService.createWallet(dto)
        );

        assertEquals(message, exception.getMessage());
        verifyNoInteractions(walletRepository);
        verifyNoInteractions(walletMapper);
    }

    @Test
    @DisplayName("Should throw RegistrationConflictException when document or email is already registered")
    void shouldThrowRegistrationConflictWhenDocumentOrEmailAlreadyExists() {
        CreateWalletDTO dto = new CreateWalletDTO("John Doe", CPF, "john.doe@email.com", PASSWORD, WalletType.COMMON);

        when(walletRepository.existsByDocumentOrEmail(dto.document(), dto.email())).thenReturn(true);

        RegistrationConflictException exception = assertThrows(
                RegistrationConflictException.class,
                () -> walletService.createWallet(dto)
        );

        assertEquals("Unable to create wallet due to duplicate document or email.", exception.getMessage());
        verify(walletRepository).existsByDocumentOrEmail(CPF, "john.doe@email.com");
        verify(walletRepository, never()).save(any(Wallet.class));
        verifyNoInteractions(walletMapper);
    }
}