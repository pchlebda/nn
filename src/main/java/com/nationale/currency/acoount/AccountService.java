package com.nationale.currency.acoount;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import java.util.function.Supplier;

import com.nationale.currency.acoount.exception.AccountNotFoundException;
import com.nationale.currency.acoount.exception.InsufficientFundsException;
import com.nationale.currency.acoount.exception.InvalidExchangeException;
import com.nationale.currency.nbp.NbpApiClient;
import com.nationale.generated.model.Account;
import com.nationale.generated.model.AccountExchange;
import com.nationale.generated.model.RegisterAccountRequest;
import com.nationale.generated.model.RegisterAccountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private static final String USD = "USD";
    private static final String PLN = "PLN";
    private static final Set<String> ALLOWED_CURRENCY = Set.of(USD, PLN);
    private final AccountRepository accountRepository;
    private final Supplier<String> idGenerator;
    private final NbpApiClient nbpApiClient;

    @Transactional
    public RegisterAccountResponse register(RegisterAccountRequest registerAccountRequest) {

        var accountEntity = AccountEntity.builder()
                .firstName(registerAccountRequest.getFirstName())
                .lastName(registerAccountRequest.getLastName())
                .balancePLN(BigDecimal.valueOf(registerAccountRequest.getBalancePLN()))
                .balanceUSD(BigDecimal.ZERO)
                .apiKey(idGenerator.get())
                .build();

        var saved = accountRepository.save(accountEntity);

        return RegisterAccountResponse.builder()
                .firstName(saved.getFirstName())
                .lastName(saved.getLastName())
                .balancePLN(saved.getBalancePLN().doubleValue())
                .balanceUSD(saved.getBalanceUSD().doubleValue())
                .apiKey(saved.getApiKey())
                .build();
    }

    @Transactional
    public Account getStatus(String apiKey) {
        return accountRepository.findById(apiKey)
                .map(this::build)
                .orElseThrow(() -> new AccountNotFoundException("Api key not found."));
    }

    @Transactional
    public Account exchange(String apiKey, AccountExchange exchange) {

        if (!isValidExchange(exchange)) {
            throw new InvalidExchangeException("Exchange must be only PLN->USD or USD->PLN.");
        }

        var accountEntity = accountRepository.findById(apiKey)
                .orElseThrow(() -> new AccountNotFoundException("Api key not found."));

        if (!hasSufficientFunds(exchange, accountEntity)) {
            throw new InsufficientFundsException("Insufficient funds.");
        }

        var usd = nbpApiClient.findExchange(USD);
        var currentUsdRate = usd.getFirst();
        var amount = BigDecimal.valueOf(exchange.getAmount());

        var balancePLN = accountEntity.getBalancePLN();
        var balanceUSD = accountEntity.getBalanceUSD();

        if (exchange.getFrom().equalsIgnoreCase(USD)) {
            var amountToTransfer = currentUsdRate.multiply(amount);
            accountEntity.setBalancePLN(balancePLN.add(amountToTransfer));
            accountEntity.setBalanceUSD(balanceUSD.subtract(amount));
        } else {
            var amountToTransfer = amount.divide(currentUsdRate, 4, RoundingMode.HALF_UP);
            accountEntity.setBalancePLN(balancePLN.subtract(amount));
            accountEntity.setBalanceUSD(balanceUSD.add(amountToTransfer));
        }

        var saved = accountRepository.save(accountEntity);

        return build(saved);
    }

    private Account build(AccountEntity accountEntity) {
        return Account.builder()
                .firstName(accountEntity.getFirstName())
                .lastName(accountEntity.getLastName())
                .balancePLN(accountEntity.getBalancePLN().setScale(4, RoundingMode.HALF_UP).doubleValue())
                .balanceUSD(accountEntity.getBalanceUSD().setScale(4, RoundingMode.HALF_UP).doubleValue())
                .build();
    }

    private boolean isValidExchange(AccountExchange accountExchange) {
        var fromCurrency = accountExchange.getFrom();
        var toCurrency = accountExchange.getTo();
        return ALLOWED_CURRENCY.contains(fromCurrency.toUpperCase()) &&
                ALLOWED_CURRENCY.contains(toCurrency.toUpperCase())
                && !fromCurrency.equalsIgnoreCase(toCurrency);
    }

    private boolean hasSufficientFunds(AccountExchange exchange, AccountEntity accountEntity) {
        var from = exchange.getFrom();
        var amount = BigDecimal.valueOf(exchange.getAmount());
        if (USD.equalsIgnoreCase(from)) {
            return accountEntity.getBalanceUSD().compareTo(amount) >= 0;
        } else {
            return accountEntity.getBalancePLN().compareTo(amount) >= 0;
        }
    }
}
