package com.nationale.currency.nbp;

import com.nationale.currency.acoount.exception.NbpApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class NbpApiClient {

    private static final String URL_PATTERN = "%s/%s";
    private final RestTemplate restTemplate;
    private final String nbpApiUrl;

    public NbpApiClient(RestTemplate restTemplate, @Value("${nbp.api.url}") String nbpApiUrl) {
        this.restTemplate = restTemplate;
        this.nbpApiUrl = nbpApiUrl;
        log.info("Starting NPB api client with basic url {}", nbpApiUrl);
    }

    public Exchange findExchange(String currencyCode) {
        log.info("Querying NBP api for {}. ", currencyCode);
        String url = String.format(URL_PATTERN, nbpApiUrl, currencyCode);
        try {
            return restTemplate.getForObject(url, Exchange.class);
        } catch (RestClientException e) {
            log.error("NPB api call has failed.", e);
            throw new NbpApiException("NBP api exception");
        }
    }
}
