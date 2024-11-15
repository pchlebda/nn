package com.nationale.currency.acoount;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.nationale.currency.acoount.exception.AccountNotFoundException;
import com.nationale.currency.acoount.exception.InsufficientFundsException;
import com.nationale.currency.acoount.exception.InvalidExchangeException;
import com.nationale.currency.nbp.Exchange;
import com.nationale.currency.nbp.ExchangeRate;
import com.nationale.currency.nbp.NbpApiClient;
import com.nationale.generated.model.AccountExchange;
import com.nationale.generated.model.RegisterAccountRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Smith";
    private static final double INITIAL_BALANCE_DOUBLE = 1000.0d;
    private static final BigDecimal INITIAL_BALANCE = BigDecimal.valueOf(INITIAL_BALANCE_DOUBLE);
    private static final String API_KEY = "cecb5b42-7c24-41cd-895c-1d3747c8444d";
    private static final double BALANCE_USD_DOUBLE = 100.0d;
    private static final BigDecimal BALANCE_USD = BigDecimal.valueOf(BALANCE_USD_DOUBLE);
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private Supplier<String> idGenerator;

    @Mock
    private NbpApiClient nbpApiClient;

    @InjectMocks
    private AccountService accountService;


    @Test
    void shouldRegisterNewAccount() {
        var request = RegisterAccountRequest.builder()
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .balancePLN(INITIAL_BALANCE_DOUBLE)
                .build();

        when(idGenerator.get()).thenReturn(API_KEY);
        var savedEntity = AccountEntity.builder()
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .balancePLN(INITIAL_BALANCE)
                .balanceUSD(BigDecimal.ZERO)
                .apiKey(API_KEY)
                .build();

        when(accountRepository.save(any(AccountEntity.class))).thenReturn(savedEntity);

        var response = accountService.register(request);

        assertEquals(FIRST_NAME, response.getFirstName());
        assertEquals(LAST_NAME, response.getLastName());
        assertEquals(INITIAL_BALANCE_DOUBLE, response.getBalancePLN());
        assertEquals(0.0d, response.getBalanceUSD());
        assertEquals(API_KEY, response.getApiKey());
    }

    @Test
    void shouldGetStatus() {
        var entity = AccountEntity.builder()
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .balancePLN(INITIAL_BALANCE)
                .balanceUSD(BALANCE_USD)
                .build();

        when(accountRepository.findById(API_KEY)).thenReturn(Optional.of(entity));

        var account = accountService.getStatus(API_KEY);

        assertEquals(FIRST_NAME, account.getFirstName());
        assertEquals(LAST_NAME, account.getLastName());
        assertEquals(INITIAL_BALANCE_DOUBLE, account.getBalancePLN());
        assertEquals(BALANCE_USD_DOUBLE, account.getBalanceUSD());
    }

    @Test
    void shouldThrowException_whenApiKeyNotFound() {
        when(accountRepository.findById(API_KEY)).thenReturn(Optional.empty());

        var accountNotFoundException = assertThrows(AccountNotFoundException.class,
                () -> accountService.getStatus(API_KEY));

        Assertions.assertEquals("Api key not found.", accountNotFoundException.getMessage());
    }

    @Test
    void shouldExchangeSuccessfulPLNtoUSD() {
        var exchange = new AccountExchange();
        exchange.setFrom("PLN");
        exchange.setTo("USD");
        exchange.setAmount(10.0);

        var argumentCaptor = ArgumentCaptor.forClass(AccountEntity.class);

        var entity = AccountEntity.builder()
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .balancePLN(BigDecimal.valueOf(100.0))
                .balanceUSD(BigDecimal.valueOf(0.0))
                .build();

        when(accountRepository.findById(API_KEY)).thenReturn(Optional.of(entity));
        when(nbpApiClient.findExchange("USD")).thenReturn(
                new Exchange("USD", List.of(new ExchangeRate("2024-11-15", BigDecimal.valueOf(4.122)))));
        var build = AccountEntity.builder()
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .balancePLN(BigDecimal.valueOf(90.0))
                .balanceUSD(BigDecimal.valueOf(2.426))
                .build();
        when(accountRepository.save(argumentCaptor.capture())).thenReturn(build);

        var value = accountService.exchange(API_KEY, exchange);

        var capture = argumentCaptor.getValue();

        assertEquals(FIRST_NAME, capture.getFirstName());
        assertEquals(LAST_NAME, capture.getLastName());
        assertEquals(BigDecimal.valueOf(90.0d), capture.getBalancePLN());
        assertThat(capture.getBalanceUSD()).isEqualByComparingTo(BigDecimal.valueOf(2.426d));
        assertEquals(FIRST_NAME, value.getFirstName());
        assertEquals(LAST_NAME, value.getLastName());
        assertEquals(90.0d, value.getBalancePLN());
        assertThat(value.getBalanceUSD()).isEqualByComparingTo(2.426d);
    }


    @Test
    void shouldExchangeSuccessfulUSDtoPLN() {
        var exchange = new AccountExchange();
        exchange.setFrom("USD");
        exchange.setTo("PLN");
        exchange.setAmount(10.0);

        var argumentCaptor = ArgumentCaptor.forClass(AccountEntity.class);

        var entity = AccountEntity.builder()
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .balancePLN(BigDecimal.valueOf(100.0))
                .balanceUSD(BigDecimal.valueOf(50.0))
                .build();

        when(accountRepository.findById(API_KEY)).thenReturn(Optional.of(entity));
        when(nbpApiClient.findExchange("USD")).thenReturn(
                new Exchange("USD", List.of(new ExchangeRate("2024-11-15", BigDecimal.valueOf(4.122)))));
        var build = AccountEntity.builder()
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .balancePLN(BigDecimal.valueOf(141.22))
                .balanceUSD(BigDecimal.valueOf(40))
                .build();
        when(accountRepository.save(argumentCaptor.capture())).thenReturn(build);

        var value = accountService.exchange(API_KEY, exchange);

        var capture = argumentCaptor.getValue();

        assertEquals(FIRST_NAME, capture.getFirstName());
        assertEquals(LAST_NAME, capture.getLastName());
        assertThat(capture.getBalancePLN()).isEqualByComparingTo(BigDecimal.valueOf(141.22));
        assertThat(capture.getBalanceUSD()).isEqualByComparingTo(BigDecimal.valueOf(40));
        assertEquals(FIRST_NAME, value.getFirstName());
        assertEquals(LAST_NAME, value.getLastName());
        assertThat(value.getBalancePLN()).isEqualByComparingTo(141.22d);
        assertThat(value.getBalanceUSD()).isEqualByComparingTo(40.0d);
    }

    @Test
    void shouldThrowException_whenInsufficientFunds() {
        var exchange = new AccountExchange();
        exchange.setFrom("USD");
        exchange.setTo("PLN");
        exchange.setAmount(1000.0);

        var entity = AccountEntity.builder()
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .balancePLN(BigDecimal.valueOf(20.0))
                .balanceUSD(BigDecimal.valueOf(50.0))
                .build();

        when(accountRepository.findById(API_KEY)).thenReturn(Optional.of(entity));

        var insufficientFundsException = assertThrows(InsufficientFundsException.class,
                () -> accountService.exchange(API_KEY, exchange));
        Assertions.assertEquals("Insufficient funds.", insufficientFundsException.getMessage());
    }

    @Test
    void shouldThrowException_whenInvalidCurrency() {
        var exchange = new AccountExchange();
        exchange.setFrom("CHF");
        exchange.setTo("PLN");
        exchange.setAmount(1000.0);

        var invalidExchangeException = assertThrows(InvalidExchangeException.class,
                () -> accountService.exchange(API_KEY, exchange));
        Assertions.assertEquals("Exchange must be only PLN->USD or USD->PLN.", invalidExchangeException.getMessage());
    }

    @Test
    void shouldThrowException_whenExchangeAPIKeyNotFound() {
        var exchange = new AccountExchange();
        exchange.setFrom("USD");
        exchange.setTo("PLN");
        exchange.setAmount(15.0);

        when(accountRepository.findById(API_KEY)).thenReturn(Optional.empty());

        var accountNotFoundException = assertThrows(AccountNotFoundException.class,
                () -> accountService.exchange(API_KEY, exchange));
        Assertions.assertEquals("Api key not found.", accountNotFoundException.getMessage());
    }
}
