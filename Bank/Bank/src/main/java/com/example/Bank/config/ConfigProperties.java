package com.example.Bank.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ConfigurationProperties(prefix = "")
@PropertySource("classpath:secrets.properties")
@Getter
@Setter
public class ConfigProperties {
    @Value("${frontend.api.url}")
    private String frontendBaseUrl;
}
