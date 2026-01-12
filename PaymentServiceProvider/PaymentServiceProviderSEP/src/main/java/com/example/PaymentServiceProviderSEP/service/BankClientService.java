package com.example.PaymentServiceProviderSEP.service;

import com.example.PaymentServiceProviderSEP.config.ConfigProperties;
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
public class BankClientService {

    private final ConfigProperties config;
    private final RestClient restClient;

    public BankClientService(ConfigProperties config, RestClient.Builder builder) {
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
                    .baseUrl(config.getBankBaseUrl())
                    .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Could not initialize secure RestClient for Bank", e);
        }
    }

    public Map<String, Object> createPaymentTransaction(String merchantId, Double amount, String currency, String STAN, Timestamp pspTimestamp) {
        Map<String, Object> requestBody = Map.of(
                "merchantId", merchantId,
                "amount", amount,
                "currency", currency,
                "STAN", STAN,
                "pspTimestamp", pspTimestamp.toString()
        );

        try {
            Map<String, Object> response = restClient.post()
                    .uri("/api/bank/payment/create")
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("paymentId") && response.containsKey("paymentUrl")) {
                return response;
            }

            throw new RuntimeException("Invalid response from Bank when creating payment transaction");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create payment transaction in Bank: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> generateQrPaymentTransaction(String merchantId, Double amount, String currency, String STAN, Timestamp pspTimestamp) {
        Map<String, Object> requestBody = Map.of(
                "merchantId", merchantId,
                "amount", amount,
                "currency", currency,
                "STAN", STAN,
                "pspTimestamp", pspTimestamp.toString()
        );
        try {
            Map<String, Object> response = restClient.post()
                    .uri("/api/bank/qr/create")
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);
            if (response != null && response.containsKey("paymentId") && response.containsKey("paymentUrl")) {
                return response;
            }

            throw new RuntimeException("Invalid response from Bank when create payment transaction");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create qr code payment in Bank");
        }
    }

    public String getMerchantIdFromBankForAccountNumber(String merchantAccountNumber) {
        Map<String, Object> requestBody = Map.of(
                "accountNumber", merchantAccountNumber
        );

        try {
            Map<String, Object> response = restClient.post()
                    .uri("/api/bank/accounts/merchant-id")
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("merchantId")) {
                return (String) response.get("merchantId");
            }

            throw new RuntimeException("Merchant ID not found in Bank for account number: " + merchantAccountNumber);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get merchant ID from Bank: " + e.getMessage(), e);
        }
    }
}
