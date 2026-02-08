package com.example.PaymentServiceProviderSEP.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class PublicClientService {
    private final RestClient publicClient;

    public PublicClientService() {
        this.publicClient = RestClient.create();
    }

    public Double getConversationResult(String url) {
        try {
            Map<String, Object> response = publicClient.get()
                    .uri(url)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("conversion_result")) {
                return Double.valueOf(response.get("conversion_result").toString());
            }

            throw new RuntimeException("Invalid response from Bank when fetching exchange rate");
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch exchange rate from Bank: " + e.getMessage(), e);
        }
    }
}
