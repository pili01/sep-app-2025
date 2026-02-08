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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MerchantSubscriptionController.class);

    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<List<SubscriptionResponseDTO>> getMerchantSubscriptions(@PathVariable Long merchantId) {
        log.info("Fetching subscriptions for merchantId={}", merchantId);
        try {
            var subscriptions = service.getSubscriptionsByMerchantId(merchantId);
            log.info("Fetched {} subscriptions for merchantId={}", subscriptions.size(), merchantId);
            return ResponseEntity.ok(subscriptions);
        } catch (Exception e) {
            log.error("Error fetching subscriptions for merchantId={}", merchantId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/available/{merchantId}")
    public ResponseEntity<List<PaymentMethodDTO>> getAvailableForMerchant(@PathVariable Long merchantId) {
        log.info("Fetching available payment methods for merchantId={}", merchantId);
        try {
            var methods = service.getAvailablePaymentMethodsForMerchant(merchantId)
                    .stream()
                    .map(pm -> new PaymentMethodDTO(
                            pm.getId(),
                            pm.getName(),
                            pm.getPaymentMethodCode().name(),
                            pm.isActive(),
                            pm.isEnabled(),
                            pm.getLastHeartbeat()
                    ))
                    .toList();
            log.info("Fetched {} available payment methods for merchantId={}", methods.size(), merchantId);
            return ResponseEntity.ok(methods);
        } catch (Exception e) {
            log.error("Error fetching available payment methods for merchantId={}", merchantId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/merchant/{merchantId}")
    public ResponseEntity<SubscriptionResponseDTO> create(@PathVariable Long merchantId,
                                                          @RequestBody @Valid SubscriptionRequestDTO dto) {
        log.info("Creating subscription for merchantId={} with paymentMethodId={}", merchantId, dto.getPaymentMethodId());
        try {
            SubscriptionResponseDTO saved = service.createSubscription(merchantId, dto);
            log.info("Subscription created successfully: subscriptionId={}", saved.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            log.error("Error creating subscription for merchantId={}: {}", merchantId, e.getMessage(), e);
            throw new RuntimeException("Error creating subscription: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionResponseDTO> update(@PathVariable Long id, @RequestBody UpdateSubRequestDTO dto) {
        log.info("Updating subscription id={} enabled={} configJson={}", id, dto.getEnabled(), dto.getConfigJson());
        try {
            SubscriptionResponseDTO updated = service.updateSubscription(id, dto.getEnabled(), dto.getConfigJson());
            log.info("Subscription updated successfully: subscriptionId={}", id);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error updating subscription id={}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("Deleting subscription id={}", id);
        try {
            service.deleteSubscription(id);
            log.info("Subscription deleted successfully: subscriptionId={}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting subscription id={}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
