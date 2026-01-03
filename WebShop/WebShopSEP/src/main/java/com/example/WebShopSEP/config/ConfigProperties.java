package com.example.WebShopSEP.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.UUID;

@Configuration
@ConfigurationProperties(prefix = "")
@PropertySource("classpath:secrets.properties")
@Getter @Setter
public class ConfigProperties {
    private String masterKey;
    private String merchantId;
    private String merchantPassword;
    private String merchantBaseUrl;
    private String merchantHandshakeEndpoint;

    @Value("${server.ssl.key-store}")
    private String keyStore;

    @Value("${server.ssl.key-store-type}")
    private String keyStoreType;

    @Value("${server.ssl.key-store-password}")
    private String keyStorePassword;
}