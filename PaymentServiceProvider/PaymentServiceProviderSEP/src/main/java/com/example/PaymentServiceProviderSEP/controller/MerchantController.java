package com.example.PaymentServiceProviderSEP.controller;

import com.example.PaymentServiceProviderSEP.dto.merchant.CreateMerchantDTO;
import com.example.PaymentServiceProviderSEP.dto.merchant.HandshakeRequestDTO;
import com.example.PaymentServiceProviderSEP.model.Merchant;
import com.example.PaymentServiceProviderSEP.service.MerchantService;
import jakarta.validation.Valid;
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

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@Valid @RequestBody CreateMerchantDTO dto) {
        try {
            Merchant merchant = merchantService.create(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(merchant);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ex.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Merchant>> getAll() {
        return ResponseEntity.ok(merchantService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Merchant> getById(@PathVariable Long id) {
        return ResponseEntity.ok(merchantService.getById(id));
    }

    @PostMapping("/handshake")
    public ResponseEntity<?> checkHealth(@RequestBody HandshakeRequestDTO request) {
        boolean isValid = merchantService.verifyCredentials(
                request.getMerchantId(),
                request.getMerchantPassword()
        );

        if (isValid) {
            return ResponseEntity.ok(Map.of("status", "UP", "message", "Connection established"));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "DOWN", "message", "Invalid Merchant Credentials"));
        }
    }
}
