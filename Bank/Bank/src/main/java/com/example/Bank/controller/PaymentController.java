package com.example.Bank.controller;

import com.example.Bank.config.ConfigProperties;
import com.example.Bank.dto.*;
import com.example.Bank.dto.payment.CreatePaymentRequest;
import com.example.Bank.dto.payment.PaymentDetailsResponse;
import com.example.Bank.dto.payment.PaymentProcessRequest;
import com.example.Bank.dto.payment.PaymentProcessResponse;
import com.example.Bank.service.PaymentService;
import com.example.Bank.util.AuditLogger;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bank/payment")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;
    private final ConfigProperties configProperties;

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private final AuditLogger audit;

    @GetMapping("/test/create")
    @PostMapping("/test/create")
    public ResponseEntity<?> createTestTransaction() {
        var transaction = paymentService.createTestTransaction();
        return ResponseEntity.ok("Test transaction created. PaymentId: " + transaction.getPaymentId() +
                "\nPayment URL: https://lap-ter-dp:4203/payment/" + transaction.getPaymentId());
    }


    //@GetMapping("/test/create-account-card")
    @PostMapping("/test/create-account-card")
    public ResponseEntity<?> createTestAccountAndCard() {
        String result = paymentService.createTestAccountAndCard();
        return ResponseEntity.ok(result);
    }

    // uri za kreiranje placanja
    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        try {
            var transaction = paymentService.createPaymentTransaction(
                    request.getAmount(),
                    request.getCurrency(),
                    request.getMerchantId(),
                    request.getSTAN()
            );

            String msg = String.format(
                    "Transaction created: id=%d, amount=%.2f, currency=%s, merchantId=%s, STAN=%s",
                    transaction.getId(),
                    transaction.getAmount(),
                    transaction.getCurrency(),
                    transaction.getMerchantId(),
                    transaction.getStan()
                    );

            log.info("Transaction created: {}", transaction);
            audit.info(msg);

            String paymentUrl = configProperties.getFrontendBaseUrl() + "/payment/" + transaction.getPaymentId();
            return ResponseEntity.ok(Map.of("paymentId", transaction.getPaymentId(), "paymentUrl", paymentUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentDetailsResponse> getPaymentDetails(@PathVariable String paymentId) {
        try {
            PaymentDetailsResponse response = paymentService.getPaymentDetails(paymentId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }


    @PostMapping("/{paymentId}/process")
    public ResponseEntity<PaymentProcessResponse> processPayment(
            @PathVariable String paymentId,
            @Valid @RequestBody PaymentProcessRequest request) {

        log.info("Process card payment request");

        PaymentProcessResponse response = paymentService.processPayment(paymentId, request);

        if (response.getSuccess()) {
            log.info("Process payment successful");
            return ResponseEntity.ok(response);
        } else {
            log.info("Process payment failed");
            return ResponseEntity.badRequest().body(response);
        }
    }


    @PostMapping("/processQR")
    public ResponseEntity<PaymentProcessResponse> processPayment(
             @RequestBody QrCodeData request) {

        log.info("Process qr code payment request");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth != null ? auth.getName() : "unknown";

        PaymentProcessResponse response = paymentService.processPaymentQR(request, email);

        return ResponseEntity.ok(response);
    }
}

