package com.nationale.currency;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.nationale.currency.nbp.Exchange;
import com.nationale.currency.nbp.ExchangeRate;
import com.nationale.currency.nbp.NbpApiClient;
import com.nationale.generated.model.Account;
import com.nationale.generated.model.AccountExchange;
import com.nationale.generated.model.RegisterAccountRequest;
import com.nationale.generated.model.RegisterAccountResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(classes = CurrencyExchangeApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CurrencyExchangeApplicationTests {

    private static final String FIRST_NAME = "Mark";
    private static final String LAST_NAME = "Green";
    private static final double INITIAL_BALANCE_DOUBLE = 1000.0d;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private NbpApiClient nbpApiClient;

    private String baseUrl() {
        return "http://localhost:" + port + "/account";
    }

    @Test
    void shouldRegisterAccount() {
        var result = registerNewAccount();

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        var body = result.getBody();
        assertNotNull(body);
        assertEquals(FIRST_NAME, body.getFirstName());
        assertEquals(LAST_NAME, body.getLastName());
        assertEquals(INITIAL_BALANCE_DOUBLE, body.getBalancePLN());
        assertThat(body.getBalanceUSD()).isEqualByComparingTo(0.0d);
        assertNotNull(body.getApiKey());
    }

    @Test
    void shouldFailRegisterAccountWithoutFirstName() {
        var request = new RegisterAccountRequest();
        request.setLastName(LAST_NAME);
        request.setBalancePLN(INITIAL_BALANCE_DOUBLE);

        var result = restTemplate.postForEntity(baseUrl(), request,
                String.class);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        var body = result.getBody();
        assertNotNull(body);
        assertEquals("{\"firstName\":\"must not be null\"}", body);
    }

    @Test
    void shouldFailRegisterAccountWithNegativeBalance() {
        var request = new RegisterAccountRequest();
        request.setFirstName(FIRST_NAME);
        request.setLastName(LAST_NAME);
        request.setBalancePLN(-100.00d);

        var result = restTemplate.postForEntity(baseUrl(), request,
                String.class);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        var body = result.getBody();
        assertNotNull(body);
        assertEquals("{\"balancePLN\":\"must be greater than or equal to 0.0\"}", body);
    }

    @Test
    void shouldGetAccountState() {
        var account = registerNewAccount().getBody();
        var headers = new HttpHeaders();
        headers.add("x-api-key", account.getApiKey());
        var requestEntity = new HttpEntity<>(headers);

        var result = restTemplate.exchange(baseUrl(), HttpMethod.GET, requestEntity, Account.class);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        var body = result.getBody();
        assertNotNull(body);
        assertEquals(FIRST_NAME, body.getFirstName());
        assertEquals(LAST_NAME, body.getLastName());
        assertEquals(INITIAL_BALANCE_DOUBLE, body.getBalancePLN());
        assertThat(body.getBalanceUSD()).isEqualByComparingTo(0.0d);
    }

    @Test
    void shouldGetBadRequest_WhenApiTokenInvalid() {
        var headers = new HttpHeaders();
        headers.add("x-api-key", "invalid");
        var requestEntity = new HttpEntity<>(headers);

        var result = restTemplate.exchange(baseUrl(), HttpMethod.GET, requestEntity, Void.class);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void shouldExchangeCurrency() {
        var account = registerNewAccount().getBody();
        var headers = new HttpHeaders();
        headers.add("x-api-key", account.getApiKey());

        var exchange = AccountExchange.builder()
                .from("PLN")
                .to("USD")
                .amount(10.0)
                .build();
        var requestEntity = new HttpEntity<>(exchange, headers);

        Mockito.when(nbpApiClient.findExchange("USD"))
                .thenReturn(new Exchange("USD", List.of(new ExchangeRate("2024-11-15", BigDecimal.valueOf(4.108)))));

        var result = restTemplate.exchange(baseUrl() + "/exchange", HttpMethod.POST, requestEntity, Account.class);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        var body = result.getBody();
        assertNotNull(body);
        assertEquals(FIRST_NAME, body.getFirstName());
        assertEquals(LAST_NAME, body.getLastName());
        assertEquals(990.0d, body.getBalancePLN());
        assertEquals(2.4343, body.getBalanceUSD(), 0.0001);
    }

    @Test
    void shouldFailOnInsufficientFunds() {
        var account = registerNewAccount().getBody();
        var headers = new HttpHeaders();
        headers.add("x-api-key", account.getApiKey());

        var exchange = AccountExchange.builder()
                .from("USD")
                .to("PLN")
                .amount(10000.0)
                .build();
        var requestEntity = new HttpEntity<>(exchange, headers);

        Mockito.when(nbpApiClient.findExchange("USD"))
                .thenReturn(new Exchange("USD", List.of(new ExchangeRate("2024-11-15", BigDecimal.valueOf(4.108)))));

        var result = restTemplate.exchange(baseUrl() + "/exchange", HttpMethod.POST, requestEntity, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        var body = result.getBody();
        assertNotNull(body);
        assertEquals("Insufficient funds.", body.get("message"));
    }

    private ResponseEntity<RegisterAccountResponse> registerNewAccount() {
        var request = new RegisterAccountRequest();
        request.setFirstName(FIRST_NAME);
        request.setLastName(LAST_NAME);
        request.setBalancePLN(INITIAL_BALANCE_DOUBLE);

        return restTemplate.postForEntity(baseUrl(), request,
                RegisterAccountResponse.class);
    }
}
