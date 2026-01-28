package com.example.PaymentServiceProviderSEP.controller;

import com.example.PaymentServiceProviderSEP.dto.payment.PaymentInitRequestDTO;
import com.example.PaymentServiceProviderSEP.dto.payment.PaymentInitResponseDTO;
import com.example.PaymentServiceProviderSEP.dto.payment.PaymentStatusDTO;
import com.example.PaymentServiceProviderSEP.dto.subscription.SubscriptionResponseDTO;
import com.example.PaymentServiceProviderSEP.model.MerchantPaymentMethodSubscription;
import com.example.PaymentServiceProviderSEP.model.PaymentMethodCode;
import com.example.PaymentServiceProviderSEP.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
    private final BankService bankService;
    private final PaymentMethodService paymentMethodService;
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


    public record PaymentInitiateRequest(
            @NotNull Long transactionId,
            @NotNull Long subscriptionId
    ) {
    }

    // metoda refaktorisana da radi za sad
    // za buduci razvoj je potrebno izbaciti grananje na osnovu enuma
    // i staviti da se request prebacuje na microservis
    // koji se dobija iz PaymentMethod-a koji je odabrao korisnik
    @PostMapping("/initiate")
    public ResponseEntity<?> initiatePayment(@Valid @RequestBody PaymentInitiateRequest request) {
        try {
            MerchantPaymentMethodSubscription subscription = merchantPaymentMethodSubscriptionService.getSubscriptionById(request.subscriptionId);

            PaymentMethodCode paymentMethodCode = subscription.getPaymentMethod().getPaymentMethodCode();
            PaymentInitResponseDTO paymentInitResponseDTO = null;
            switch (paymentMethodCode) {
                case BANK_CARD, BANK_QR -> {
                    paymentInitResponseDTO = bankService.requestPaymentParametersFromBank(subscription, request.transactionId);
                }
                case CUSTOM -> {
                }
                default -> throw new IllegalArgumentException("Unsupported payment method code");
            }
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
            var transaction = paymentService.getTransactionByTransactionId(transactionId);
            Map<String, Object> response = Map.of(
                    "status", transaction.getStatus().toString(),
                    "paymentMethod", transaction.getPaymentMethod() != null ? transaction.getPaymentMethod().toString() : null
            );
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }
}
