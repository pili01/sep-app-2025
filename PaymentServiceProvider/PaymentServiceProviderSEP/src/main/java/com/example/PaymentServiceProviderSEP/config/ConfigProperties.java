package com.example.PaymentServiceProviderSEP.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ConfigurationProperties(prefix = "")
@PropertySource("classpath:secrets.properties")
@Getter @Setter
public class ConfigProperties {
    private String masterKey;

    @Value("${bank.api.url}")
    private String bankBaseUrl;

    @Value("${frontend.api.url}")
    private String frontendBaseUrl;

    @Value("${server.ssl.key-store}")
    private String keyStore;

    @Value("${server.ssl.key-store-type}")
    private String keyStoreType;

    @Value("${server.ssl.key-store-password}")
    private String keyStorePassword;

    @Value("${exchange.rate.api.url}")
    private String exchangeRateApiUrl;
}
