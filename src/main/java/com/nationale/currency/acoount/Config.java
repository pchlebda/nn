package com.nationale.currency.acoount;

import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class Config {

    @Bean
    Supplier<String> idGenerator() {
        return () -> UUID.randomUUID().toString();
    }

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
