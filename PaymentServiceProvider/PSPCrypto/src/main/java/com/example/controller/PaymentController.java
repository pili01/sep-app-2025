package com.example.controller;

import com.example.dto.PaymentRequest;
import com.example.dto.TransactionStatusResponse;
import com.example.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;


    @PostMapping("/pay")
    public ResponseEntity<?> initiatePayment(@Valid @RequestBody PaymentRequest request) {
        log.info("Received payment request for transaction: {}", request.getTransactionId());
        try {
            var response = paymentService.initiatePayment(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Payment initiation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage()));
        } catch (UnsupportedOperationException e) {
            log.error("Crypto payment not yet implemented: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(
                    Map.of("error", "Crypto payment not yet implemented"));
        } catch (Exception e) {
            log.error("Unexpected error during payment initiation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", "Internal server error"));
        }
    }


    @GetMapping("/status/{transactionId}")
    public ResponseEntity<TransactionStatusResponse> checkStatus(@PathVariable String transactionId) {
        log.info("Checking status for transaction: {}", transactionId);
        try {
            TransactionStatusResponse response = paymentService.getTransactionStatus(transactionId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Transaction not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Unexpected error during status check", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
