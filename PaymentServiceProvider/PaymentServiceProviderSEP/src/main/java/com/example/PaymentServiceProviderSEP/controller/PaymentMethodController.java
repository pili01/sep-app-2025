package com.example.PaymentServiceProviderSEP.controller;

import com.example.PaymentServiceProviderSEP.dto.paymentMethod.PaymentMethodConnectRequest;
import com.example.PaymentServiceProviderSEP.dto.paymentMethod.PaymentMethodDTO;
import com.example.PaymentServiceProviderSEP.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @GetMapping
    public ResponseEntity<List<PaymentMethodDTO>> getAll() {
        return ResponseEntity.ok(paymentMethodService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentMethodDTO> getById(@PathVariable Long id) {
        PaymentMethodDTO dto = paymentMethodService.getById(id);
        if (dto != null) {
            return ResponseEntity.ok(dto);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<PaymentMethodDTO> create(
            @RequestBody PaymentMethodConnectRequest request) {
        PaymentMethodDTO created = paymentMethodService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentMethodDTO> update(
            @PathVariable Long id,
            @RequestBody PaymentMethodConnectRequest request) {
        PaymentMethodDTO updated = paymentMethodService.update(id, request);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        paymentMethodService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<PaymentMethodDTO> toggleEnabled(@PathVariable Long id) {
        PaymentMethodDTO updated = paymentMethodService.toggleEnabled(id);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping(value = "/upload-icon", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadIcon(@RequestParam("file") MultipartFile file) {
        return paymentMethodService.uploadIcon(file);
    }

    @Scheduled(fixedRateString = "${heartbeat.fixed-rate-ms}")
    public void heartbeat() {
        paymentMethodService.heartbeatRoundRobin();
    }
}