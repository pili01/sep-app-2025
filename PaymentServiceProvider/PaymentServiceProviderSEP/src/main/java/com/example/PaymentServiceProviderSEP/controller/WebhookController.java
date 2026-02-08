package com.example.PaymentServiceProviderSEP.controller;

import com.example.PaymentServiceProviderSEP.dto.CheckStatusRequest;
import com.example.PaymentServiceProviderSEP.dto.CheckStatusResponse;
import com.example.PaymentServiceProviderSEP.dto.WebhookPaymentRequest;
import com.example.PaymentServiceProviderSEP.model.TransactionStatus;
import com.example.PaymentServiceProviderSEP.service.MerchantVerificationService;
import com.example.PaymentServiceProviderSEP.service.WebhookVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final WebhookVerificationService webhookVerificationService;
    private final MerchantVerificationService merchantVerificationService;

    @PostMapping("/payment-notification")
    public ResponseEntity<Void> receivePaymentNotification(@RequestBody WebhookPaymentRequest request) {
        log.info("Received webhook notification for transaction: {}", request.getTransactionId());
        log.info("Webhook data - Status: {}, Amount: {}, Currency: {}",
                request.getStatus(), request.getAmount(), request.getCurrency());

        try {
            // Verifikuj i update-uj transakciju
            webhookVerificationService.verifyAndUpdateTransaction(request.getTransactionId());

            log.info("Webhook notification processed successfully for transaction: {}",
                    request.getTransactionId());
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Failed to process webhook notification for transaction {}: {}",
                    request.getTransactionId(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/check-status")
    public ResponseEntity<CheckStatusResponse> checkStatus(@RequestBody CheckStatusRequest request) {
        log.info("Received check-status request from merchant for transaction: {}", request.getTransactionId());

        try {
            CheckStatusResponse response = merchantVerificationService.verifyMerchantCheckStatus(request);
            log.info("Check-status response for merchant: verified={}, status={}",
                    response.getVerified(), response.getStatus());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to process check-status request for transaction {}: {}",
                    request.getTransactionId(), e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new CheckStatusResponse(
                            request.getTransactionId(),
                            "ERROR",
                            false,
                            null,
                            "Check-status failed: " + e.getMessage(),
                            TransactionStatus.ERROR));
        }
    }
}
