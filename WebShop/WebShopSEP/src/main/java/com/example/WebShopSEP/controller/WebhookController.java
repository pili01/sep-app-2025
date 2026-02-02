package com.example.WebShopSEP.controller;

import com.example.WebShopSEP.dto.CheckStatusRequest;
import com.example.WebShopSEP.dto.CheckStatusResponse;
import com.example.WebShopSEP.dto.WebhookPaymentRequest;
import com.example.WebShopSEP.model.TransactionStatus;
import com.example.WebShopSEP.service.WebhookVerificationService;
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

    @PostMapping("/payment-notification")
    public ResponseEntity<Void> receivePaymentNotification(@RequestBody WebhookPaymentRequest request) {
        log.info("Received webhook notification for transaction: {}", request.getTransactionId());
        log.info("Webhook data - Status: {}", request.getStatus());

        try {
            // Process webhook notification - verify payment with PSP
            webhookVerificationService.updateTransactionFromWebhook(request.getTransactionId());

            log.info("Webhook notification processed successfully for transaction: {}",
                    request.getTransactionId());
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Failed to process webhook notification for transaction {}: {}",
                    request.getTransactionId(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
