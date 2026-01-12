package com.example.Bank.service;

import com.example.Bank.config.ConfigProperties;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.security.KeyStore;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class PSPClientService {

    private final ConfigProperties config;
    private final RestClient restClient;

    public PSPClientService(ConfigProperties config, RestClient.Builder builder) {
        this.config = config;
        try {
            KeyStore trustStore = KeyStore.getInstance(config.getKeyStoreType());
            ClassPathResource resource = new ClassPathResource(config.getKeyStore().replace("classpath:", ""));
            char[] password = config.getKeyStorePassword().toCharArray();

            trustStore.load(resource.getInputStream(), password);

            var sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial(trustStore, new TrustSelfSignedStrategy())
                    .build();

            var httpClient = HttpClients.custom()
                    .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                            .setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
                                    .setSslContext(sslContext)
                                    .setHostnameVerifier((hostname, session) -> true)
                                    .build())
                            .build())
                    .build();

            this.restClient = builder
                    .baseUrl(config.getPspBaseUrl())
                    .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Could not initialize secure RestClient for PSP", e);
        }
    }

    public Optional<String> sendPaymentStatus(String stan, String globalTransactionId, LocalDateTime acquirerTimestamp, String status) {
        Map<String, Object> requestBody = Map.of(
                "stan", stan,
                "globalTransactionId", globalTransactionId,
                "acquirerTimestamp", acquirerTimestamp.toString(),
                "status", status
        );

        try {
            Map<String, Object> response = restClient.post()
                    .uri("/api/payment/status")
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("success")) {
                return Optional.empty();
            }

            if (response.containsKey("redirectUrl")) {
                return Optional.of((String) response.get("redirectUrl"));
            } else {
                return Optional.empty();
            }

        } catch (Exception e) {
            return Optional.empty();
        }
    }
}



