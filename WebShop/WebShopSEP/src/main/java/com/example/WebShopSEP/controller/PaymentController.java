package com.example.WebShopSEP.controller;

import com.example.WebShopSEP.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/status")
    public ResponseEntity<?> getPaymentStatus(@RequestParam String transactionId) {
        try {
            var transaction = paymentService.getTransactionStatus(transactionId);
            return ResponseEntity.ok(Map.of(
                    "transactionId", transaction.getTransactionId(),
                    "status", transaction.getStatus().toString(),
                    "rentalId", transaction.getRentalId()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", "Internal server error"));
        }
    }
}



