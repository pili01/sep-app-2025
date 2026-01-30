package com.example.PaymentServiceProviderSEP.controller;

import com.example.PaymentServiceProviderSEP.dto.paymentMethod.PaymentMethodDTO;
import com.example.PaymentServiceProviderSEP.dto.subscription.SubscriptionRequestDTO;
import com.example.PaymentServiceProviderSEP.dto.subscription.SubscriptionResponseDTO;
import com.example.PaymentServiceProviderSEP.dto.subscription.UpdateSubRequestDTO;
import com.example.PaymentServiceProviderSEP.model.MerchantPaymentMethodSubscription;
import com.example.PaymentServiceProviderSEP.service.MerchantPaymentMethodSubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class MerchantSubscriptionController {

    private final MerchantPaymentMethodSubscriptionService service;

    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<List<SubscriptionResponseDTO>> getMerchantSubscriptions(@PathVariable Long merchantId) {
        return ResponseEntity.ok( service.getSubscriptionsByMerchantId(merchantId));
    }

    @GetMapping("/available/{merchantId}")
    public ResponseEntity<List<PaymentMethodDTO>> getAvailableForMerchant(
            @PathVariable Long merchantId) {
        return ResponseEntity.ok(
                service.getAvailablePaymentMethodsForMerchant(merchantId)
                        .stream()
                        .map(pm -> new PaymentMethodDTO(
                                pm.getId(),
                                pm.getName(),
                                pm.getPaymentMethodCode().name(),
                                pm.isActive(),
                                pm.isEnabled(),
                                pm.getLastHeartbeat()
                        ))
                        .toList()
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/merchant/{merchantId}")
    public ResponseEntity<SubscriptionResponseDTO> create(
            @PathVariable Long merchantId,
            @RequestBody @Valid SubscriptionRequestDTO dto) {
        try{
            SubscriptionResponseDTO saved = service.createSubscription(
                    merchantId, dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            throw new RuntimeException("Error creating subscription: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionResponseDTO> update(
            @PathVariable Long id,
            @RequestBody UpdateSubRequestDTO dto) {

        SubscriptionResponseDTO updated = service.updateSubscription(
                id, dto.getEnabled(), dto.getConfigJson());

        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteSubscription(id);
        return ResponseEntity.noContent().build();
    }
}
