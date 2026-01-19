package com.example.PaymentServiceProviderSEP.controller;

import com.example.PaymentServiceProviderSEP.dto.paymentMethod.PaymentMethodConnectRequest;
import com.example.PaymentServiceProviderSEP.model.PaymentMethod;
import com.example.PaymentServiceProviderSEP.repository.PaymentMethodRepository;
import com.example.PaymentServiceProviderSEP.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @PostMapping("/connect")
    public ResponseEntity<Void> connect(
            @RequestBody PaymentMethodConnectRequest request) {

        if (paymentMethodService.existsByName(request.getName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        paymentMethodService.connect(request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Scheduled(fixedRate = 15_000)
    public void heartbeat() {
        paymentMethodService.heartbeatRoundRobin();
    }
}