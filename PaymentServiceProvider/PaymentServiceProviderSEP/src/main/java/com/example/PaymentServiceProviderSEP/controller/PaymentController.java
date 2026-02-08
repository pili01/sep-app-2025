package com.example.PaymentServiceProviderSEP.controller;

import com.example.PaymentServiceProviderSEP.dto.payment.PaymentInitRequestDTO;
import com.example.PaymentServiceProviderSEP.dto.payment.PaymentInitResponseDTO;
import com.example.PaymentServiceProviderSEP.dto.payment.PaymentStatusDTO;
import com.example.PaymentServiceProviderSEP.model.MerchantPaymentMethodSubscription;
import com.example.PaymentServiceProviderSEP.model.PaymentMethodCode;
import com.example.PaymentServiceProviderSEP.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
    private final PayService payService;
    private final MerchantPaymentMethodSubscriptionService merchantPaymentMethodSubscriptionService;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PaymentController.class);

    @PostMapping("/init")
    public ResponseEntity<?> initializePayment(@Valid @RequestBody PaymentInitRequestDTO request) {
        log.info("Initializing payment for merchantId={} amount={} {}", request.getMerchantId(), request.getAmount(), request.getCurrency());
        try {
            Map<?, ?> response = paymentService.initializePayment(request);
            log.info("Payment initialized successfully for merchantId={}", request.getMerchantId());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.warn("Invalid merchant during payment initialization: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during payment initialization", e);
            return ResponseEntity.internalServerError().body(java.util.Map.of("error", "Internal server error"));
        }
    }

    public record PaymentInitiateRequest(
            @NotNull Long transactionId,
            @NotNull Long subscriptionId
    ) {
    }

    @PostMapping("/initiate")
    public ResponseEntity<?> initiatePayment(@Valid @RequestBody PaymentInitiateRequest request) {
        log.info("Initiating payment transactionId={} subscriptionId={}", request.transactionId, request.subscriptionId);
        try {
            MerchantPaymentMethodSubscription subscription = merchantPaymentMethodSubscriptionService.getSubscriptionById(request.subscriptionId);
            PaymentInitResponseDTO paymentInitResponseDTO = payService.requestPaymentParameters(subscription, request.transactionId);
            log.info("Payment initiation successful for transactionId={}", request.transactionId);
            return ResponseEntity.ok(paymentInitResponseDTO);
        } catch (RuntimeException e) {
            log.warn("Runtime exception during payment initiation: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/status")
    public ResponseEntity<?> receivePaymentStatus(@Valid @RequestBody PaymentStatusDTO statusDTO) {
        log.info("Receiving payment status for globalTransactionId={} status={}", statusDTO.getGlobalTransactionId(), statusDTO.getStatus());
        try {
            String redirectUrl = paymentService.processPaymentStatus(statusDTO);
            log.info("Payment status processed successfully for globalTransactionId={}", statusDTO.getGlobalTransactionId());
            return ResponseEntity.ok(Map.of("success", true, "redirectUrl", redirectUrl));
        } catch (RuntimeException e) {
            log.warn("Runtime exception while processing payment status for globalTransactionId={}: {}", statusDTO.getGlobalTransactionId(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error while processing payment status for globalTransactionId={}", statusDTO.getGlobalTransactionId(), e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", "Internal server error"));
        }
    }

    @GetMapping("/transaction/{transactionId}/status")
    public ResponseEntity<?> getTransactionStatus(@PathVariable String transactionId) {
        log.info("Fetching payment transaction status for transactionId={}", transactionId);
        try {
            var transaction = paymentService.getTransactionByTransactionId(transactionId);
            Map<String, Object> response = Map.of(
                    "status", transaction.getStatus().toString(),
                    "paymentMethod", transaction.getPaymentMethod() != null ? transaction.getPaymentMethod().toString() : null
            );
            log.info("Transaction status fetched successfully for transactionId={}", transactionId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.warn("Runtime exception while fetching transaction status for transactionId={}: {}", transactionId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error while fetching transaction status for transactionId={}", transactionId, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }
}
