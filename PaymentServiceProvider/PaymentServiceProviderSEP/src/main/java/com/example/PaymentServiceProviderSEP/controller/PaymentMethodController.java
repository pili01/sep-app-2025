package com.example.PaymentServiceProviderSEP.controller;

import com.example.PaymentServiceProviderSEP.dto.paymentMethod.PaymentMethodConnectRequest;
import com.example.PaymentServiceProviderSEP.dto.paymentMethod.PaymentMethodDTO;
import com.example.PaymentServiceProviderSEP.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(PaymentMethodController.class);

    @GetMapping
    public ResponseEntity<List<PaymentMethodDTO>> getAll() {
        return ResponseEntity.ok(paymentMethodService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentMethodDTO> getById(@PathVariable Long id) {
        log.info("Fetching payment method with id={}", id);
        PaymentMethodDTO dto = paymentMethodService.getById(id);
        if (dto != null) {
            log.info("Found payment method with id={}", id);
            return ResponseEntity.ok(dto);
        }
        log.warn("Payment method not found with id={}", id);
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<PaymentMethodDTO> create(
            @RequestBody PaymentMethodConnectRequest request) {
        log.info("Creating payment method with name={}", request.getName());
        PaymentMethodDTO created = paymentMethodService.create(request);
        log.info("Created payment method with id={}", created.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentMethodDTO> update(
            @PathVariable Long id,
            @RequestBody PaymentMethodConnectRequest request) {
        log.info("Updating payment method id={} with name={}", id, request.getName());
        PaymentMethodDTO updated = paymentMethodService.update(id, request);
        if (updated != null) {
            log.info("Updated payment method id={}", id);
            return ResponseEntity.ok(updated);
        }
        log.warn("Payment method not found for update, id={}", id);
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("Deleting payment method id={}", id);
        paymentMethodService.delete(id);
        log.info("Deleted payment method id={}", id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<PaymentMethodDTO> toggleEnabled(@PathVariable Long id) {
        log.info("Toggling enabled for payment method id={}", id);
        PaymentMethodDTO updated = paymentMethodService.toggleEnabled(id);
        if (updated != null) {
            log.info("Toggled enabled for payment method id={}, now enabled={}", id, updated.getEnabled());
            return ResponseEntity.ok(updated);
        }
        log.warn("Payment method not found for toggle, id={}", id);
        return ResponseEntity.notFound().build();
    }

    @PostMapping(value = "/upload-icon", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadIcon(@RequestParam("file") MultipartFile file) {
        log.info("Uploading icon: {}", file.getOriginalFilename());
        ResponseEntity<?> response = paymentMethodService.uploadIcon(file);
        log.info("Upload icon response: {}", response.getStatusCode());
        return response;
    }

    @Scheduled(fixedRateString = "${heartbeat.fixed-rate-ms}")
    public void heartbeat() {
        paymentMethodService.heartbeatRoundRobin();
    }
}