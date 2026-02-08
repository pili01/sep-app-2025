package com.example.Bank.controller;

import com.example.Bank.config.ConfigProperties;
import com.example.Bank.dto.payment.CreatePaymentRequest;
import com.example.Bank.service.PaymentService;
import com.example.Bank.util.AuditLogger;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/bank/qr")
@RequiredArgsConstructor
public class QrCodeController {
    private final PaymentService paymentService;
    private final ConfigProperties configProperties;

    private static final Logger log = LoggerFactory.getLogger(QrCodeController.class);

    // uri za kreiranje placanja
    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        try {
            var transaction = paymentService.createPaymentTransaction(
                    request.getAmount(),
                    request.getCurrency(),
                    request.getMerchantId(),
                    request.getSTAN()
            );

            log.info("Created QR payment transaction: paymentId={}, amount={}, currency={}, merchantId={}",
                    transaction.getPaymentId(),
                    transaction.getAmount(),
                    transaction.getCurrency(),
                    transaction.getMerchantId());

            String paymentUrl = configProperties.getFrontendBaseUrl() + "/payment-qr/" + transaction.getPaymentId();
            return ResponseEntity.ok(Map.of("paymentId", transaction.getPaymentId(), "paymentUrl", paymentUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/generate/{paymentId}")
    public ResponseEntity<?> generateQrCode(@PathVariable String paymentId) {
        try {
            var qrCode = paymentService.generateQrCodeForPaymentTransaction(paymentId);

            log.info("Generated QR code for paymentId={}", paymentId);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"ips-qr.png\"")
                    .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePrivate())
                    .body(qrCode);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
