package com.example.PaymentServiceProviderSEP.controller;

import com.example.PaymentServiceProviderSEP.dto.subscription.SubscriptionRequestDTO;
import com.example.PaymentServiceProviderSEP.dto.subscription.SubscriptionResponseDTO;
import com.example.PaymentServiceProviderSEP.dto.subscription.UpdateSubRequestDTO;
import com.example.PaymentServiceProviderSEP.model.MerchantPaymentMethodSubscription;
import com.example.PaymentServiceProviderSEP.service.MerchantPaymentMethodSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class MerchantSubscriptionController {

    private final MerchantPaymentMethodSubscriptionService service;

    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<List<SubscriptionResponseDTO>> getMerchantSubscriptions(@PathVariable Long merchantId) {
        List<SubscriptionResponseDTO> dtos = service.getSubscriptionsByMerchantId(merchantId)
                .stream()
                .map(this::convertToDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/merchant/{merchantId}")
    public ResponseEntity<SubscriptionResponseDTO> create(
            @PathVariable Long merchantId,
            @RequestBody SubscriptionRequestDTO dto) {

        MerchantPaymentMethodSubscription saved = service.createSubscription(
                merchantId, dto.getPaymentMethodCode(), dto.getConfigJson());

        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionResponseDTO> update(
            @PathVariable Long id,
            @RequestBody UpdateSubRequestDTO dto) {

        MerchantPaymentMethodSubscription updated = service.updateSubscription(
                id, dto.getEnabled(), dto.getConfigJson());

        return ResponseEntity.ok(convertToDto(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteSubscription(id);
        return ResponseEntity.noContent().build();
    }

    private SubscriptionResponseDTO convertToDto(MerchantPaymentMethodSubscription entity) {
        return new SubscriptionResponseDTO(
                entity.getId(),
                entity.getMerchant().getId(),
                entity.getPaymentMethodCode(),
                entity.getEnabled(),
                entity.getConfigJson(),
                entity.getCreatedAt()
        );
    }
}
