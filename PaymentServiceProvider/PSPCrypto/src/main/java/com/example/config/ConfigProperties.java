package com.example.config;

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

    @Value("${psp.core.api.url}")
    private String pspCoreApiUrl;

    @Value("${psp.core.connect.endpoint}")
    private String pspCoreConnectEndpoint;

    @Value("${server.ssl.key-store}")
    private String keyStore;

    @Value("${server.ssl.key-store-type}")
    private String keyStoreType;

    @Value("${server.ssl.key-store-password}")
    private String keyStorePassword;

    @Value("${exchange.rate.api.url}")
    private String exchangeRateApiUrl;

    @Value("${bitcoin.api.url:https://api.blockcypher.com/v1/btc/test3}")
    private String bitcoinApiUrl;

    @Value("${blockstream.api.url:https://blockstream.info/testnet/api}")
    private String blockstreamApiUrl;

    @Value("${server.port}")
    private int serverPort;

    @Value("${psp.frontend.url:https://localhost:4202}")
    private String pspFrontendUrl;
}
