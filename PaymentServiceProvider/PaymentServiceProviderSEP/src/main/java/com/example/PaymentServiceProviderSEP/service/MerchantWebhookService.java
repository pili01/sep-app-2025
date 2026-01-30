package com.example.PaymentServiceProviderSEP.service;

import com.example.PaymentServiceProviderSEP.model.Merchant;
import com.example.PaymentServiceProviderSEP.model.Transaction;
import com.example.PaymentServiceProviderSEP.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import com.example.PaymentServiceProviderSEP.config.ConfigProperties;

import java.security.KeyStore;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantWebhookService {

    private final MerchantRepository merchantRepository;
    private final ConfigProperties config;
    private RestClient restClient;

    private RestClient getRestClient() {
        if (restClient == null) {
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

                restClient = RestClient.builder()
                        .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
                        .build();
            } catch (Exception e) {
                throw new RuntimeException("Could not initialize secure RestClient for webhook", e);
            }
        }
        return restClient;
    }

    public void notifyMerchant(Transaction transaction) {
        try {
            log.info("Sending webhook notification to merchant for transaction: {}", transaction.getTransactionId());

            // Get merchant
            Merchant merchant = merchantRepository.findByMerchantId(transaction.getMerchantId())
                    .orElseThrow(() -> new RuntimeException("Merchant not found: " + transaction.getMerchantId()));

            Map<String, Object> payload = Map.of(
                    "transactionId", transaction.getTransactionId(),
                    "status", transaction.getStatus().toString(),
                    "amount", transaction.getAmount(),
                    "currency", transaction.getCurrency(),
                    "pspTimestamp", transaction.getPspTimestamp().toString());

            getRestClient().post()
                    .uri(merchant.getWebHookUrl())
                    .body(payload)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        log.error("Webhook notification to merchant failed with status: {}", response.getStatusCode());
                    })
                    .toBodilessEntity();

            log.info("Webhook notification sent successfully to merchant");
        } catch (Exception e) {
            log.error("Failed to send webhook notification to merchant for transaction {}: {}",
                    transaction.getTransactionId(), e.getMessage(), e);
        }
    }
}
