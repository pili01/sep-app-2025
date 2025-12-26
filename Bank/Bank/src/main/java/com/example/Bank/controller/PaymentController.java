package com.example.Bank.controller;

import com.example.Bank.dto.PaymentDetailsResponse;
import com.example.Bank.dto.PaymentProcessRequest;
import com.example.Bank.dto.PaymentProcessResponse;
import com.example.Bank.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bank/payment")
@CrossOrigin(origins = "*")
public class PaymentController {
    
    private final PaymentService paymentService;
    
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    

    @GetMapping("/test/create")
    @PostMapping("/test/create")
    public ResponseEntity<?> createTestTransaction() {
        var transaction = paymentService.createTestTransaction();
        return ResponseEntity.ok("Test transaction created. PaymentId: " + transaction.getPaymentId() + 
            "\nPayment URL: https://localhost:4203/payment/" + transaction.getPaymentId());
    }
    

    @GetMapping("/test/create-account-card")
    @PostMapping("/test/create-account-card")
    public ResponseEntity<?> createTestAccountAndCard() {
        String result = paymentService.createTestAccountAndCard();
        return ResponseEntity.ok(result);
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

