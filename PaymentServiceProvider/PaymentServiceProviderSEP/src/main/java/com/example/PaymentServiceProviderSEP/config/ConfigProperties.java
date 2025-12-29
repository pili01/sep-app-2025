package com.example.PaymentServiceProviderSEP.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:secrets.properties")
@ConfigurationProperties(prefix = "")
@Getter
@Setter
public class ConfigProperties {
    private String masterKey;
}
