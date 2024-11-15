package com.nationale.currency.acoount;

import com.nationale.generated.api.AccountApi;
import com.nationale.generated.model.Account;
import com.nationale.generated.model.AccountExchange;
import com.nationale.generated.model.RegisterAccountRequest;
import com.nationale.generated.model.RegisterAccountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class AccountApiController implements AccountApi {

    private final AccountService accountService;

    @Override
    public ResponseEntity<Account> exchangeCurrency(String xApiKey, AccountExchange accountExchange) {
        return ResponseEntity.ok(accountService.exchange(xApiKey, accountExchange));
    }

    @Override
    public ResponseEntity<Account> getAccountStatus(String xApiKey) {
        return ResponseEntity.ok(accountService.getStatus(xApiKey));
    }

    @Override
    public ResponseEntity<RegisterAccountResponse> registerAccount(RegisterAccountRequest registerAccountRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.register(registerAccountRequest));
    }

}
