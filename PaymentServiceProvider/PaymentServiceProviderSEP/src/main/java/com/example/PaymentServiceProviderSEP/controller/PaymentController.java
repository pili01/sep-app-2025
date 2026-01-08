package com.example.PaymentServiceProviderSEP.controller;

import com.example.PaymentServiceProviderSEP.dto.payment.PaymentInitRequestDTO;
import com.example.PaymentServiceProviderSEP.dto.payment.PaymentInitResponseDTO;
import com.example.PaymentServiceProviderSEP.dto.payment.PaymentStatusDTO;
import com.example.PaymentServiceProviderSEP.dto.subscription.SubscriptionResponseDTO;
import com.example.PaymentServiceProviderSEP.service.MerchantPaymentMethodSubscriptionService;
import com.example.PaymentServiceProviderSEP.service.MerchantService;
import com.example.PaymentServiceProviderSEP.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final MerchantPaymentMethodSubscriptionService merchantPaymentMethodSubscriptionService;

    @PostMapping("/init")
    public ResponseEntity<?> initializePayment(@Valid @RequestBody PaymentInitRequestDTO request) {
        try {
            Map<?, ?> response = paymentService.initializePayment(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(java.util.Map.of("error", "Internal server error"));
        }
    }

    @GetMapping("/subscriptions/{merchantId}")
    public ResponseEntity<?> getMerchantSubscriptions(@PathVariable Long merchantId) {
        List<String> dtos = merchantPaymentMethodSubscriptionService.getActiveSubscriptionsByMerchantId(merchantId)
                .stream()
                .map(sub -> sub.getPaymentMethodCode().name())
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/initiate")
    public ResponseEntity<?> initiatePayment(@RequestBody Map<String, String> request) {
        try {
            PaymentInitResponseDTO paymentInitResponseDTO = paymentService.requestPaymentParametersFromBank(request);
            return ResponseEntity.ok(paymentInitResponseDTO);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/status")
    public ResponseEntity<?> receivePaymentStatus(@Valid @RequestBody PaymentStatusDTO statusDTO) {
        try {
            String redirectUrl = paymentService.processPaymentStatus(statusDTO);
            return ResponseEntity.ok(Map.of("success", true, "redirectUrl", redirectUrl));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", "Internal server error"));
        }
    }

    @GetMapping("/transaction/{transactionId}/status")
    public ResponseEntity<?> getTransactionStatus(@PathVariable String transactionId) {
        try {
            var status = paymentService.getTransactionStatusByTransactionId(transactionId);
            return ResponseEntity.ok(Map.of("status", status.toString()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }
}
