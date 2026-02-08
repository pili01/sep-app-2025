package com.example.service;

import com.example.dto.*;
import com.example.model.Transaction;
import com.example.model.TransactionStatus;
import com.example.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

        private final TransactionRepository transactionRepository;
        private final WebhookService webhookService;
        private final PayPalClient payPalClient;

        @Value("${server.port}")
        private String serverPort;

        @Transactional
        public Map<String, String> initiatePayment(PaymentRequest request) {
                log.info("Initiating PayPal payment for transaction: {}", request.getTransactionId());

                // Check if transaction already exists
                if (transactionRepository.findByPspTransactionId(request.getTransactionId()).isPresent()) {
                        throw new IllegalArgumentException(
                                        "Transaction can be initialized only one time: " + request.getTransactionId());
                }

                // Parse PayPal config from merchant config JSON
                PayPalConfig payPalConfig = payPalClient.parseConfig(request.getMerchantConfig());

                if (payPalConfig.getClientId() == null || payPalConfig.getSecret() == null) {
                        throw new IllegalArgumentException(
                                        "PayPal clientId and secret are required in merchant config");
                }

                // Get PayPal access token
                String accessToken = payPalClient.getAccessToken(payPalConfig.getClientId(), payPalConfig.getSecret());

                // Create transaction record
                Transaction transaction = new Transaction();
                transaction.setPspTransactionId(request.getTransactionId());
                transaction.setAmount(request.getAmount());
                transaction.setCurrency(request.getCurrency());
                transaction.setStatus(TransactionStatus.PENDING);
                transaction.setMerchantConfig(request.getMerchantConfig());
                transaction.setWebhookUrl(request.getWebhookUrl());
                transaction.setSuccessUrl(request.getSuccessUrl());
                transaction.setFailedUrl(request.getFailedUrl());
                transaction.setErrorUrl(request.getErrorUrl());
                transaction.setClientId(payPalConfig.getClientId());
                transaction.setSecret(payPalConfig.getSecret());
                transaction.setAccessToken(accessToken);

                // Generate return and cancel URLs
                String returnUrl = "https://lap-ter-dp:" + serverPort + "/api/payment/complete?transactionId="
                                + transaction.getPspTransactionId();
                String cancelUrl = "https://lap-ter-dp:" + serverPort + "/api/payment/cancel?transactionId="
                                + transaction.getPspTransactionId();

                // Create PayPal order
                PayPalOrderResponse orderResponse = payPalClient.createOrder(
                                accessToken,
                                transaction.getPspTransactionId(),
                                request.getAmount(),
                                request.getCurrency(),
                                returnUrl,
                                cancelUrl);

                transaction.setGlobalTransactionId(orderResponse.getId());
                transactionRepository.save(transaction);

                // Get approval URL from PayPal response
                String approvalUrl = orderResponse.getApprovalUrl();
                if (approvalUrl == null) {
                        throw new RuntimeException("PayPal did not return approval URL");
                }

                log.info("PayPal payment initiated successfully. Transaction ID: {}, PayPal Order ID: {}",
                                transaction.getPspTransactionId(), orderResponse.getId());

                return Map.of("paymentId", orderResponse.getId(), "paymentUrl", approvalUrl);
        }

        @Transactional(readOnly = true)
        public TransactionStatusResponse getTransactionStatus(String transactionId) {
                log.info("Checking status for transaction: {}", transactionId);

                Transaction transaction = transactionRepository.findByPspTransactionId(transactionId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Transaction not found: " + transactionId));

                return TransactionStatusResponse.builder()
                                .transactionId(transaction.getPspTransactionId())
                                .status(transaction.getStatus().name())
                                .amount(transaction.getAmount())
                                .currency(transaction.getCurrency())
                                .createdAt(transaction.getCreatedAt())
                                .completedAt(transaction.getCompletedAt())
                                .errorMessage(transaction.getErrorMessage())
                                .build();
        }

        @Transactional
        public String completePayment(String pspTransactionId, String payerId) {
                log.info("Completing payment for local transaction: {}, PayerID: {}", pspTransactionId, payerId);

                Transaction transaction = transactionRepository.findByPspTransactionId(pspTransactionId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Transaction not found: " + pspTransactionId));

                try {
                        // Capture the PayPal order payment
                        PayPalCaptureResponse captureResponse = payPalClient.captureOrder(
                                        transaction.getAccessToken(),
                                        transaction.getGlobalTransactionId());

                        // Check if capture was successful
                        if (captureResponse == null || !"COMPLETED".equals(captureResponse.getStatus())) {
                                log.error("PayPal capture failed for transaction: {}, Status: {}",
                                                pspTransactionId,
                                                captureResponse != null ? captureResponse.getStatus() : "null");

                                // Update transaction status to ERROR
                                transaction.setStatus(TransactionStatus.ERROR);
                                transaction.setErrorMessage("PayPal capture failed: " +
                                                (captureResponse != null ? captureResponse.getStatus()
                                                                : "Unknown error"));
                                transaction.setCompletedAt(LocalDateTime.now());
                                transactionRepository.save(transaction);

                                // Notify PSP Core via webhook
                                webhookService.notifyPaymentError(transaction);

                                // Return error URL
                                return transaction.getFailedUrl();
                        }

                        // Payment captured successfully
                        transaction.setStatus(TransactionStatus.COMPLETED);
                        transaction.setPaypalPayerId(payerId);
                        transaction.setCompletedAt(LocalDateTime.now());
                        transactionRepository.save(transaction);

                        // Notify PSP Core via webhook
                        webhookService.notifyPaymentCompleted(transaction);

                        log.info("Payment completed successfully for transaction: {}",
                                        transaction.getPspTransactionId());
                        return transaction.getSuccessUrl();

                } catch (Exception e) {
                        log.error("Error capturing PayPal payment for transaction: {}", pspTransactionId, e);

                        // Update transaction status to ERROR
                        transaction.setStatus(TransactionStatus.ERROR);
                        transaction.setErrorMessage("PayPal capture exception: " + e.getMessage());
                        transaction.setCompletedAt(LocalDateTime.now());
                        transactionRepository.save(transaction);

                        // Notify PSP Core via webhook
                        webhookService.notifyPaymentError(transaction);

                        // Return error URL
                        return transaction.getFailedUrl();
                }
        }

        @Transactional
        public String failPayment(String pspTransactionId, String errorMessage) {
                log.info("Failing payment for local transaction: {}", pspTransactionId);

                Transaction transaction = transactionRepository.findByPspTransactionId(pspTransactionId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Transaction not found: " + pspTransactionId));

                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setErrorMessage(errorMessage);
                transaction.setCompletedAt(LocalDateTime.now());

                transactionRepository.save(transaction);

                // Notify PSP Core via webhook
                webhookService.notifyPaymentFailed(transaction);

                log.info("Payment failed for transaction: {}", transaction.getPspTransactionId());
                return transaction.getFailedUrl();
        }
}
