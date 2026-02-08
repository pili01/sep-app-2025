package com.example.controller;

import com.example.dto.PaymentRequest;
import com.example.dto.PaymentResponse;
import com.example.dto.TransactionStatusResponse;
import com.example.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Initiate a new payment
     * POST /api/payment/pay
     */
    @PostMapping("/pay")
    public ResponseEntity<?> initiatePayment(@Valid @RequestBody PaymentRequest request) {
        log.info("Received payment request for transaction: {}", request.getTransactionId());
        try {
            var response = paymentService.initiatePayment(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Payment initiation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    PaymentResponse.builder()
                            .transactionId(request.getTransactionId())
                            .status("ERROR")
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Unexpected error during payment initiation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    PaymentResponse.builder()
                            .transactionId(request.getTransactionId())
                            .status("ERROR")
                            .message("Internal server error")
                            .build());
        }
    }

    /**
     * Check payment status
     * GET /api/payment/status/{transactionId}
     */
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

    /**
     * Complete payment (simulated PayPal redirect)
     * GET /api/payment/complete
     */
    @GetMapping("/complete")
    public ResponseEntity<String> completePayment(
            @RequestParam String token,
            @RequestParam String transactionId,
            @RequestParam(required = false) String PayerID) {

        log.info("Payment completion callback - OrderID: {}, TransactionID: {}, PayerID: {}",
                token, transactionId, PayerID);

        try {
            String successUrl = paymentService.completePayment(transactionId, PayerID);
            return ResponseEntity
                    .status(HttpStatus.FOUND) // 302
                    .header(HttpHeaders.LOCATION, successUrl)
                    .build();
        } catch (Exception e) {
            log.error("Error completing payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error completing payment: " + e.getMessage());
        }
    }

    /**
     * Cancel payment
     * GET /api/payment/cancel
     */
    @GetMapping("/cancel")
    public ResponseEntity<String> cancelPayment(@RequestParam String transactionId) {
        log.info("Payment cancelled for transaction: {}", transactionId);
        try {
            String failedUrl = paymentService.failPayment(transactionId, "Payment cancelled by user");
            return ResponseEntity
                    .status(HttpStatus.FOUND) // 302
                    .header(HttpHeaders.LOCATION, failedUrl)
                    .build();
        } catch (Exception e) {
            log.error("Error cancelling payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error cancelling payment: " + e.getMessage());
        }
    }
}
