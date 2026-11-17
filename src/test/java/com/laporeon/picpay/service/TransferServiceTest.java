package com.laporeon.picpay.service;

import com.laporeon.picpay.dtos.request.NotificationRequestDTO;
import com.laporeon.picpay.dtos.request.TransferRequestDTO;
import com.laporeon.picpay.dtos.response.TransferResponseDTO;
import com.laporeon.picpay.entity.Transfer;
import com.laporeon.picpay.entity.Wallet;
import com.laporeon.picpay.enums.WalletType;
import com.laporeon.picpay.exception.custom.ClientException;
import com.laporeon.picpay.exception.custom.InsufficientBalanceException;
import com.laporeon.picpay.exception.custom.TransferNotAllowedException;
import com.laporeon.picpay.exception.custom.UnauthorizedException;
import com.laporeon.picpay.exception.custom.WalletNotFoundException;
import com.laporeon.picpay.mapper.TransferMapper;
import com.laporeon.picpay.repository.TransferRepository;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransferService Tests")
class TransferServiceTest {
    private static final UUID PAYER_ID = UUID.fromString("a3b0c02c-1e34-4b7e-9a9f-cf8a7ef77d1e");
    private static final UUID PAYEE_ID = UUID.fromString("f97dde36-9fa2-4f7c-8c3d-93efb61fbc79");

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private TransferMapper transferMapper;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TransferService transferService;

    static Stream<Arguments> successfulTransfers() {
        return Stream.of(
                //        scenario                         payee type             payer    payee    amount   payer after  payee after
                arguments("COMMON to SHOPKEEPER",          WalletType.SHOPKEEPER, "500.00", "100.00", "150.00", "350.00", "250.00"),
                arguments("COMMON to COMMON",              WalletType.COMMON,     "500.00", "0.00",   "150.00", "350.00", "150.00"),
                arguments("payer balance equals amount",   WalletType.COMMON,     "50.00",  "0.00",   "50.00",  "0.00",   "50.00")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("successfulTransfers")
    @DisplayName("Should transfer amount from payer to payee after authorization and persist the transfer")
    void shouldTransferSuccessfully(String scenario, WalletType payeeType,
                                    String payerBalance, String payeeBalance, String amountValue,
                                    String expectedPayerBalance, String expectedPayeeBalance) {
        BigDecimal amount = new BigDecimal(amountValue);
        Wallet payer = wallet(PAYER_ID, WalletType.COMMON, payerBalance);
        Wallet payee = wallet(PAYEE_ID, payeeType, payeeBalance);

        TransferRequestDTO dto = new TransferRequestDTO(amount, PAYER_ID, PAYEE_ID);
        Transfer transfer = new Transfer(payee, payer, amount);
        TransferResponseDTO expectedResponse = new TransferResponseDTO(amount, PAYER_ID, PAYEE_ID);

        when(walletRepository.findById(PAYER_ID)).thenReturn(Optional.of(payer));
        when(walletRepository.findById(PAYEE_ID)).thenReturn(Optional.of(payee));
        when(transferMapper.toEntity(payee, payer, amount)).thenReturn(transfer);
        when(transferRepository.save(transfer)).thenReturn(transfer);
        when(transferMapper.toResponseDTO(transfer)).thenReturn(expectedResponse);

        TransferResponseDTO response = transferService.transfer(dto);

        assertSame(expectedResponse, response);
        assertBalance(payer, expectedPayerBalance);
        assertBalance(payee, expectedPayeeBalance);

        InOrder order = inOrder(walletRepository, authorizationService, transferMapper, transferRepository, eventPublisher);
        order.verify(walletRepository).findById(PAYER_ID);
        order.verify(walletRepository).findById(PAYEE_ID);
        order.verify(authorizationService).ensureAuthorized();
        order.verify(transferMapper).toEntity(payee, payer, amount);
        order.verify(transferRepository).save(transfer);
        order.verify(eventPublisher).publishEvent(
                new NotificationRequestDTO(payer.getFullName(), payee.getFullName(), amount)
        );
        order.verify(transferMapper).toResponseDTO(transfer);
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Should throw WalletNotFoundException when payer wallet does not exist")
    void shouldThrowWalletNotFoundWhenPayerDoesNotExist() {
        TransferRequestDTO dto = new TransferRequestDTO(new BigDecimal("100.00"), PAYER_ID, PAYEE_ID);

        when(walletRepository.findById(PAYER_ID)).thenReturn(Optional.empty());

        WalletNotFoundException exception = assertThrows(
                WalletNotFoundException.class,
                () -> transferService.transfer(dto)
        );

        assertEquals("No wallet found for id '" + PAYER_ID + "'.", exception.getMessage());
        verify(walletRepository).findById(PAYER_ID);
        verify(walletRepository, never()).findById(PAYEE_ID);
        verify(walletRepository, never()).save(any(Wallet.class));
        verifyNoInteractions(authorizationService);
        verifyNoInteractions(transferMapper);
        verifyNoInteractions(transferRepository);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Should throw WalletNotFoundException when payee wallet does not exist")
    void shouldThrowWalletNotFoundWhenPayeeDoesNotExist() {
        TransferRequestDTO dto = new TransferRequestDTO(new BigDecimal("100.00"), PAYER_ID, PAYEE_ID);
        Wallet payer = wallet(PAYER_ID, WalletType.COMMON, "500.00");

        when(walletRepository.findById(PAYER_ID)).thenReturn(Optional.of(payer));
        when(walletRepository.findById(PAYEE_ID)).thenReturn(Optional.empty());

        WalletNotFoundException exception = assertThrows(
                WalletNotFoundException.class,
                () -> transferService.transfer(dto)
        );

        assertEquals("No wallet found for id '" + PAYEE_ID + "'.", exception.getMessage());
        assertBalance(payer, "500.00");
        verify(walletRepository, never()).save(any(Wallet.class));
        verifyNoInteractions(authorizationService);
        verifyNoInteractions(transferMapper);
        verifyNoInteractions(transferRepository);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Should throw TransferNotAllowedException when payer is a SHOPKEEPER")
    void shouldThrowTransferNotAllowedWhenPayerIsShopkeeper() {
        TransferRequestDTO dto = new TransferRequestDTO(new BigDecimal("100.00"), PAYER_ID, PAYEE_ID);
        Wallet payer = wallet(PAYER_ID, WalletType.SHOPKEEPER, "1000.00");
        Wallet payee = wallet(PAYEE_ID, WalletType.COMMON, "0.00");

        when(walletRepository.findById(PAYER_ID)).thenReturn(Optional.of(payer));
        when(walletRepository.findById(PAYEE_ID)).thenReturn(Optional.of(payee));

        TransferNotAllowedException exception = assertThrows(
                TransferNotAllowedException.class,
                () -> transferService.transfer(dto)
        );

        assertEquals("Shopkeeper accounts are not allowed to transfer.", exception.getMessage());
        assertBalance(payer, "1000.00");
        assertBalance(payee, "0.00");
        verify(walletRepository, never()).save(any(Wallet.class));
        verifyNoInteractions(authorizationService);
        verifyNoInteractions(transferMapper);
        verifyNoInteractions(transferRepository);
        verifyNoInteractions(eventPublisher);
    }

    static Stream<Arguments> insufficientBalances() {
        return Stream.of(
                //        scenario                       payer    amount
                arguments("balance lower than amount",   "99.99", "100.00"),
                arguments("balance zero",                "0.00",  "3.00")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("insufficientBalances")
    @DisplayName("Should throw InsufficientBalanceException when payer balance is lower than the amount")
    void shouldThrowInsufficientBalance(String scenario, String payerBalance, String amount) {
        TransferRequestDTO dto = new TransferRequestDTO(new BigDecimal(amount), PAYER_ID, PAYEE_ID);
        Wallet payer = wallet(PAYER_ID, WalletType.COMMON, payerBalance);
        Wallet payee = wallet(PAYEE_ID, WalletType.COMMON, "0.00");

        when(walletRepository.findById(PAYER_ID)).thenReturn(Optional.of(payer));
        when(walletRepository.findById(PAYEE_ID)).thenReturn(Optional.of(payee));

        InsufficientBalanceException exception = assertThrows(
                InsufficientBalanceException.class,
                () -> transferService.transfer(dto)
        );

        assertEquals("Unable to complete transfer due to insufficient balance.", exception.getMessage());
        assertBalance(payer, payerBalance);
        assertBalance(payee, "0.00");
        verify(walletRepository, never()).save(any(Wallet.class));
        verifyNoInteractions(authorizationService);
        verifyNoInteractions(transferMapper);
        verifyNoInteractions(transferRepository);
        verifyNoInteractions(eventPublisher);
    }

    static Stream<Arguments> authorizationFailures() {
        return Stream.of(
                //        scenario                              exception                                                  message
                arguments("authorization denied",             new UnauthorizedException(),                               "Transfer rejected by authorization service."),
                arguments("authorization service unavailable", new ClientException("Failed to call authorization service."), "Failed to call authorization service.")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("authorizationFailures")
    @DisplayName("Should propagate the authorization exception and not move balances nor persist the transfer")
    void shouldNotTransferWhenAuthorizationFails(String scenario, RuntimeException authorizationException, String message) {
        TransferRequestDTO dto = new TransferRequestDTO(new BigDecimal("100.00"), PAYER_ID, PAYEE_ID);
        Wallet payer = wallet(PAYER_ID, WalletType.COMMON, "500.00");
        Wallet payee = wallet(PAYEE_ID, WalletType.COMMON, "0.00");

        when(walletRepository.findById(PAYER_ID)).thenReturn(Optional.of(payer));
        when(walletRepository.findById(PAYEE_ID)).thenReturn(Optional.of(payee));
        doThrow(authorizationException).when(authorizationService).ensureAuthorized();

        RuntimeException exception = assertThrows(
                authorizationException.getClass(),
                () -> transferService.transfer(dto)
        );

        assertSame(authorizationException, exception);
        assertEquals(message, exception.getMessage());
        assertBalance(payer, "500.00");
        assertBalance(payee, "0.00");
        verify(authorizationService).ensureAuthorized();
        verify(walletRepository, never()).save(any(Wallet.class));
        verifyNoInteractions(transferMapper);
        verifyNoInteractions(transferRepository);
        verifyNoInteractions(eventPublisher);
    }

    private static Wallet wallet(UUID id, WalletType type, String balance) {
        Wallet wallet = type == WalletType.COMMON
                ? new Wallet("John Doe", "52998224725", "john.doe@email.com", "hashed", WalletType.COMMON)
                : new Wallet("Acme Store", "11222333000181", "contact@acme.com", "hashed", WalletType.SHOPKEEPER);
        ReflectionTestUtils.setField(wallet, "id", id);

        BigDecimal initialBalance = new BigDecimal(balance);
        if (initialBalance.signum() > 0) {
            wallet.credit(initialBalance);
        }
        return wallet;
    }

    private static void assertBalance(Wallet wallet, String expected) {
        BigDecimal value = new BigDecimal(expected);
        assertTrue(wallet.isBalanceGreaterOrEqual(value),
                "Expected balance of " + expected + " but it was lower");
        assertFalse(wallet.isBalanceGreaterOrEqual(value.add(new BigDecimal("0.01"))),
                "Expected balance of " + expected + " but it was higher");
    }
}