package com.example.service;

import com.example.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayPalClient {

    private final ObjectMapper objectMapper;
    private static final String PAYPAL_SANDBOX_URL = "https://api.sandbox.paypal.com";

    /**
     * Get PayPal access token using OAuth2
     */
    public String getAccessToken(String clientId, String secret) {
        log.info("Getting PayPal access token");

        String auth = clientId + ":" + secret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        RestClient restClient = RestClient.create();

        try {
            PayPalTokenResponse response = restClient.post()
                    .uri(PAYPAL_SANDBOX_URL + "/v1/oauth2/token")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .body("grant_type=client_credentials")
                    .retrieve()
                    .body(PayPalTokenResponse.class);

            if (response != null && response.getAccessToken() != null) {
                log.info("Successfully obtained PayPal access token");
                return response.getAccessToken();
            }

            throw new RuntimeException("Failed to get access token from PayPal");
        } catch (Exception e) {
            log.error("Error getting PayPal access token", e);
            throw new RuntimeException("Failed to authenticate with PayPal: " + e.getMessage(), e);
        }
    }

    /**
     * Create PayPal order
     */
    public PayPalOrderResponse createOrder(String accessToken, String transactionId, BigDecimal amount, String currency,
            String returnUrl, String cancelUrl) {
        log.info("Creating PayPal order for transaction: {}", transactionId);

        PayPalOrderRequest request = PayPalOrderRequest.builder()
                .intent("CAPTURE")
                .purchaseUnits(List.of(
                        PayPalOrderRequest.PurchaseUnit.builder()
                                .referenceId(transactionId)
                                .description("Payment for transaction " + transactionId)
                                .amount(PayPalOrderRequest.Amount.builder()
                                        .currencyCode(currency)
                                        .value(amount.toString())
                                        .build())
                                .build()))
                .applicationContext(PayPalOrderRequest.ApplicationContext.builder()
                        .returnUrl(returnUrl)
                        .cancelUrl(cancelUrl)
                        .brandName("Payment Gateway")
                        .userAction("PAY_NOW")
                        .build())
                .build();

        RestClient restClient = RestClient.create();

        try {
            PayPalOrderResponse response = restClient.post()
                    .uri(PAYPAL_SANDBOX_URL + "/v2/checkout/orders")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(request)
                    .retrieve()
                    .body(PayPalOrderResponse.class);

            if (response != null && response.getId() != null) {
                log.info("Successfully created PayPal order: {}", response.getId());
                return response;
            }

            throw new RuntimeException("Invalid response from PayPal when creating order");
        } catch (Exception e) {
            log.error("Error creating PayPal order", e);
            throw new RuntimeException("Failed to create PayPal order: " + e.getMessage(), e);
        }
    }

    /**
     * Parse PayPal config from JSON string
     */
    public PayPalConfig parseConfig(String configJson) {
        try {
            return objectMapper.readValue(configJson, PayPalConfig.class);
        } catch (Exception e) {
            log.error("Error parsing PayPal config", e);
            throw new RuntimeException("Invalid PayPal configuration: " + e.getMessage(), e);
        }
    }

    /**
     * Capture PayPal order payment
     */
    public PayPalCaptureResponse captureOrder(String accessToken, String orderId) {
        log.info("Capturing PayPal order: {}", orderId);

        RestClient restClient = RestClient.create();

        try {
            PayPalCaptureResponse response = restClient.post()
                    .uri(PAYPAL_SANDBOX_URL + "/v2/checkout/orders/" + orderId + "/capture")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .body(PayPalCaptureResponse.class);

            if (response != null && "COMPLETED".equals(response.getStatus())) {
                log.info("Successfully captured PayPal order: {}", orderId);
                return response;
            }

            throw new RuntimeException(
                    "PayPal capture failed with status: " + (response != null ? response.getStatus() : "null"));
        } catch (Exception e) {
            log.error("Error capturing PayPal order: {}", orderId, e);
            throw new RuntimeException("Failed to capture PayPal order: " + e.getMessage(), e);
        }
    }

    /**
     * Get PayPal order details
     */
    public PayPalOrderDetailsResponse getOrderDetails(String accessToken, String orderId) {
        log.info("Getting PayPal order details: {}", orderId);

        RestClient restClient = RestClient.create();

        try {
            PayPalOrderDetailsResponse response = restClient.get()
                    .uri(PAYPAL_SANDBOX_URL + "/v2/checkout/orders/" + orderId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(PayPalOrderDetailsResponse.class);

            if (response != null && response.getId() != null) {
                log.info("Successfully retrieved PayPal order details: {}", orderId);
                return response;
            }

            throw new RuntimeException("Invalid response from PayPal when getting order details");
        } catch (Exception e) {
            log.error("Error getting PayPal order details: {}", orderId, e);
            throw new RuntimeException("Failed to get PayPal order details: " + e.getMessage(), e);
        }
    }
}
