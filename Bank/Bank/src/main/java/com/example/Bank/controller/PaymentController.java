package com.example.Bank.controller;

import com.example.Bank.config.ConfigProperties;
import com.example.Bank.dto.CreatePaymentRequest;
import com.example.Bank.dto.PaymentDetailsResponse;
import com.example.Bank.dto.PaymentProcessRequest;
import com.example.Bank.dto.PaymentProcessResponse;
import com.example.Bank.service.AccountService;
import com.example.Bank.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bank/payment")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;
    private final ConfigProperties configProperties;

    @GetMapping("/test/create")
    @PostMapping("/test/create")
    public ResponseEntity<?> createTestTransaction() {
        var transaction = paymentService.createTestTransaction();
        return ResponseEntity.ok("Test transaction created. PaymentId: " + transaction.getPaymentId() +
                "\nPayment URL: https://localhost:4203/payment/" + transaction.getPaymentId());
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

        PaymentProcessResponse response = paymentService.processPayment(paymentId, request);

        if (response.getSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}

