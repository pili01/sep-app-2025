package com.example.PaymentServiceProviderSEP.controller;

import com.example.PaymentServiceProviderSEP.dto.merchant.CreateMerchantDTO;
import com.example.PaymentServiceProviderSEP.dto.merchant.HandshakeRequestDTO;
import com.example.PaymentServiceProviderSEP.model.Merchant;
import com.example.PaymentServiceProviderSEP.service.MerchantService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/merchants")
@CrossOrigin(origins = "*")
public class MerchantController {

    private final MerchantService merchantService;
    private static final Logger log = LoggerFactory.getLogger(MerchantController.class);

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@Valid @RequestBody CreateMerchantDTO dto) {
        log.info("Creating merchant with name={}", dto.getName());
        try {
            Merchant merchant = merchantService.create(dto);
            log.info("Merchant created successfully with id={}", merchant.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(merchant);
        } catch (IllegalArgumentException ex) {
            log.warn("Failed to create merchant: {}", ex.getMessage());
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ex.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Merchant>> getAll() {
        log.info("Fetching all merchants");
        List<Merchant> merchants = merchantService.getAll();
        log.info("Fetched {} merchants", merchants.size());
        return ResponseEntity.ok(merchants);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Merchant> getById(@PathVariable Long id) {
        log.info("Fetching merchant with id={}", id);
        Merchant merchant = merchantService.getById(id);
        log.info("Fetched merchant: id={}, name={}", merchant.getId(), merchant.getName());
        return ResponseEntity.ok(merchant);
    }

    @PostMapping("/handshake")
    public ResponseEntity<?> checkHealth(@RequestBody HandshakeRequestDTO request) {
        log.info("Handshake request for merchantId={}", request.getMerchantId());
        boolean isValid = merchantService.verifyCredentials(
                request.getMerchantId(),
                request.getMerchantPassword()
        );

        if (isValid) {
            log.info("Handshake successful for merchantId={}", request.getMerchantId());
            return ResponseEntity.ok(Map.of("status", "UP", "message", "Connection established"));
        } else {
            log.warn("Handshake failed for merchantId={}: Invalid credentials", request.getMerchantId());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "DOWN", "message", "Invalid Merchant Credentials"));
        }
    }
}
