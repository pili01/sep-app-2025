package com.example.WebShopSEP.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.UUID;

@Configuration
@PropertySource("classpath:secrets.properties")
@ConfigurationProperties(prefix = "")
@Getter
@Setter
public class ConfigProperties {
    private String masterKey;
    private String merchantServiceUrl;
    private UUID myAppMerchantId;
    private UUID myAppMerchantPassword;
}
