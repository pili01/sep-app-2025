package com.example.WebShopSEP.service;

import com.example.WebShopSEP.config.ConfigProperties;
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
public class PSPClientService {

    private final RestClient restClient;
    private final ConfigProperties config;

    public PSPClientService(RestClient.Builder builder, ConfigProperties config) {
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
                    .baseUrl(config.getMerchantBaseUrl())
                    .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Could not initialize secure RestClient", e);
        }
    }

    public void connectToMerchantBackend() {
        Map<String, Object> requestBody = Map.of(
                "merchantId", config.getMerchantId(),
                "merchantPassword", config.getMerchantPassword().toString()
        );

        System.out.println("Connecting to: " + config.getMerchantBaseUrl() + config.getMerchantHandshakeEndpoint());

        try {
            Map<String, Object> response = restClient.post()
                    .uri(config.getMerchantHandshakeEndpoint())
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response != null) {
                System.out.println("--- HANDSHAKE SUCCESS ---");
                System.out.println("Merchant Message: " + response.get("message"));
                System.out.println("Merchant Status: " + response.get("status"));
            }

        } catch (Exception e) {
            System.err.println("--- HANDSHAKE FAILED ---");
            System.err.println("Error: " + e.getMessage());
        }
    }

    public String initializePayment(Double amount, String merchantOrderId, Timestamp merchantTimestamp) {
        Map<String, Object> requestBody = Map.of(
                "merchantId", config.getMerchantId(),
                "merchantPassword", config.getMerchantPassword(),
                "amount", amount,
                "currency", config.getCurrency(),
                "merchantOrderId", merchantOrderId,
                "merchantTimestamp", merchantTimestamp
        );

        try {
            Map<String, Object> response = restClient.post()
                    .uri("/api/payment/init")
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("redirectionUrl")) {
                return (String) response.get("redirectionUrl");
            }

            throw new RuntimeException("Invalid response from PSP");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize payment with PSP: " + e.getMessage(), e);
        }
    }

    public String getTransactionStatus(String transactionId) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri("/api/payment/transaction/" + transactionId + "/status")
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("status")) {
                return (String) response.get("status");
            }

            throw new RuntimeException("Invalid response from PSP when getting transaction status");
        } catch (Exception e) {
            throw new RuntimeException("Failed to get transaction status from PSP: " + e.getMessage(), e);
        }
    }

    public Map<String, String> getTransactionStatusWithPaymentMethod(String transactionId) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri("/api/payment/transaction/" + transactionId + "/status")
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("status")) {
                String status = (String) response.get("status");
                String paymentMethod = response.containsKey("paymentMethod") && response.get("paymentMethod") != null 
                        ? (String) response.get("paymentMethod") 
                        : null;
                
                return Map.of(
                        "status", status,
                        "paymentMethod", paymentMethod != null ? paymentMethod : ""
                );
            }

            throw new RuntimeException("Invalid response from PSP when getting transaction status");
        } catch (Exception e) {
            throw new RuntimeException("Failed to get transaction status from PSP: " + e.getMessage(), e);
        }
    }
}
