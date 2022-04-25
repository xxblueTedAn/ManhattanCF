package com.xxblue.mcf.config;


import com.xxblue.mcf.utils.ExchangeRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RestApiConfig {

    @Bean
    public ExchangeRestTemplate exchangeRestTemplate() {
        ExchangeRestTemplate exchangeRestTemplate = new ExchangeRestTemplate();
        return exchangeRestTemplate;
    }

}