package com.example.controller;

import com.example.dto.CheckStatusRequest;
import com.example.dto.CheckStatusResponse;
import com.example.service.PaymentVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class WebhookController {

    private final PaymentVerificationService verificationService;

    //Endpoint koji PSP poziva da provjeri status transakcije

    @PostMapping("/check-status")
    public ResponseEntity<CheckStatusResponse> checkStatus(@RequestBody CheckStatusRequest request) {
        log.info("Received status check request from PSP for transaction: {}", request.getTransactionId());

        try {
            CheckStatusResponse response = verificationService.verifyPaymentStatus(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error checking payment status", e);
            return ResponseEntity.ok(CheckStatusResponse.builder()
                    .transactionId(request.getTransactionId())
                    .status("ERROR")
                    .verified(false)
                    .message("Error verifying payment: " + e.getMessage())
                    .build());
        }
    }
}
