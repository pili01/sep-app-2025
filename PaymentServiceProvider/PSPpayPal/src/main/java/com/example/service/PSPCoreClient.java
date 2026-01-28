package com.example.service;

import com.example.config.ConfigProperties;
import lombok.extern.slf4j.Slf4j;
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
import java.sql.Timestamp;
import java.util.Map;

@Service
@Slf4j
public class PSPCoreClient {

    private final ConfigProperties config;
    private final RestClient restClient;

    public PSPCoreClient(ConfigProperties config, RestClient.Builder builder) {
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
                    .baseUrl(config.getPspCoreApiUrl())
                    .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Could not initialize secure RestClient for Bank", e);
        }
    }

    public void connectToCorePSP() {

        String hostname = "https://localhost:" + config.getServerPort();

        Map<String, Object> requestBody = Map.of(
                "name", "PSPBank",
                "hostname", hostname,
                "statusUrl", "/api/health",
                "paymentUrl", "/api/pay",
                "paymentMethodCode", "BANK_CARD"
        );

        try {
            Map<String, Object> response = restClient.post()
                    .uri(config.getPspCoreConnectEndpoint())
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            log.info("Successfully connected to Core PSP!");
        } catch (Exception e) {
            log.error("Error connecting to Core PSP", e);
        }
    }
}
