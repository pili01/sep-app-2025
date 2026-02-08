package com.example.PaymentServiceProviderSEP.service;

import com.example.PaymentServiceProviderSEP.dto.paymentMethod.PaymentMethodConnectRequest;
import com.example.PaymentServiceProviderSEP.dto.paymentMethod.PaymentMethodDTO;
import com.example.PaymentServiceProviderSEP.model.PaymentMethod;
import com.example.PaymentServiceProviderSEP.model.PaymentMethodCode;
import com.example.PaymentServiceProviderSEP.repository.PaymentMethodRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final SecureRestClientFactory restClientFactory;
    private static final Logger log = LoggerFactory.getLogger(PaymentMethodService.class);

    @Value("${app.upload.dir:uploads/payment-method-icons}")
    private String uploadDir;

    public boolean existsByName(String name) {
        return paymentMethodRepository.existsByName(name);
    }

    private PaymentMethodCode resolvePaymentMethodCode(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            return PaymentMethodCode.CUSTOM;
        }

        try {
            return PaymentMethodCode.valueOf(rawCode.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return PaymentMethodCode.CUSTOM;
        }
    }

    @Transactional
    public void heartbeatRoundRobin() {
        log.debug("Starting heartbeat round-robin");

        paymentMethodRepository.findNextForHeartbeat(PaymentMethodCode.CUSTOM)
                .ifPresent(method -> {
                    log.info("Selected payment method {} for heartbeat", method.getId());
                    heartbeat(method);
                });

        log.debug("Completed heartbeat round-robin");
    }

    private void heartbeat(PaymentMethod method) {
        var client = restClientFactory.create(method.getHostname());
        method.setLastHeartbeat(LocalDateTime.now());
        log.info("Sending heartbeat request to payment method id={} at {}", method.getId(), method.getHostname());

        try {
            client.get()
                    .uri(method.getHealthEndpoint())
                    .retrieve()
                    .toBodilessEntity();

            method.setActive(true);
            log.info("Heartbeat successful for payment method id={}", method.getId());
        } catch (Exception e) {
            method.setActive(false);
            log.warn("Heartbeat failed for payment method id={} at {}: {}", method.getId(), method.getHostname(), e.getMessage());
        }

        paymentMethodRepository.save(method);
        log.debug("Saved heartbeat status for payment method id={}, active={}", method.getId(), method.isActive());
    }

    @Transactional
    public List<PaymentMethodDTO> getAll() {
        return paymentMethodRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional
    public PaymentMethodDTO getById(Long id) {
        return paymentMethodRepository.findById(id)
                .map(this::mapToDTO)
                .orElse(null);
    }

    @Transactional
    public PaymentMethodDTO create(PaymentMethodConnectRequest request) {
        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setName(request.getName());
        paymentMethod.setHostname(request.getHostname());
        paymentMethod.setHealthEndpoint(request.getStatusUrl());
        paymentMethod.setPaymentEndpoint(request.getPaymentUrl());
        paymentMethod.setPaymentMethodCode(resolvePaymentMethodCode(request.getPaymentMethodCode()));

        String iconPath = request.getIconPath();
        paymentMethod.setIconPath(iconPath != null && !iconPath.trim().isEmpty() ? iconPath : null);

        paymentMethod.setEnabled(request.getEnabled() != null ? request.getEnabled() : false);
        paymentMethod.setActive(request.getActive() != null ? request.getActive() : false);

        PaymentMethod saved = paymentMethodRepository.save(paymentMethod);
        return mapToDTO(saved);
    }

    @Transactional
    public PaymentMethodDTO update(Long id, PaymentMethodConnectRequest request) {
        return paymentMethodRepository.findById(id)
                .map(pm -> {
                    pm.setName(request.getName());
                    pm.setHostname(request.getHostname());
                    pm.setHealthEndpoint(request.getStatusUrl());
                    pm.setPaymentEndpoint(request.getPaymentUrl());
                    pm.setPaymentMethodCode(resolvePaymentMethodCode(request.getPaymentMethodCode()));

                    // Set iconPath, convert empty string to null
                    String iconPath = request.getIconPath();
                    pm.setIconPath(iconPath != null && !iconPath.trim().isEmpty() ? iconPath : null);

                    if (request.getEnabled() != null) {
                        pm.setEnabled(request.getEnabled());
                    }

                    return mapToDTO(paymentMethodRepository.save(pm));
                })
                .orElse(null);
    }

    @Transactional
    public void delete(Long id) {
        paymentMethodRepository.deleteById(id);
    }

    @Transactional
    public PaymentMethodDTO toggleEnabled(Long id) {
        return paymentMethodRepository.findById(id)
                .map(pm -> {
                    pm.setEnabled(!pm.isEnabled());
                    return mapToDTO(paymentMethodRepository.save(pm));
                })
                .orElse(null);
    }

    @Transactional
    public void ensureInternalPaymentMethodsExist() {
        ensurePaymentMethodExists(
                PaymentMethodCode.BANK_CARD,
                "Bank Card",
                "card.jpg"
        );

        ensurePaymentMethodExists(
                PaymentMethodCode.BANK_QR,
                "Bank QR",
                "qrCode.jpg"
        );
    }

    private void ensurePaymentMethodExists(
            PaymentMethodCode code,
            String name,
            String iconPath
    ) {
        if (paymentMethodRepository.findByPaymentMethodCode(code).isPresent()) {
            return;
        }

        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setName(name);
        paymentMethod.setPaymentMethodCode(code);

        paymentMethod.setHostname("internal");
        paymentMethod.setHealthEndpoint("internal");
        paymentMethod.setPaymentEndpoint("internal");

        paymentMethod.setActive(true);
        paymentMethod.setEnabled(true);
        paymentMethod.setIconPath(iconPath);

        paymentMethodRepository.save(paymentMethod);
    }


    public ResponseEntity<?> uploadIcon(MultipartFile file) {
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("File is empty"));
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(new ErrorResponse("File must be an image"));
            }

            // Validate file size (max 2MB)
            if (file.getSize() > 2 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(new ErrorResponse("File size must be less than 2MB"));
            }

            // Create upload directory with absolute path if it doesn't exist
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            log.info("Upload path: {}", uploadPath);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Created upload directory: {}", uploadPath);
            }

            // Generate unique filename
            String originalFileName = file.getOriginalFilename();
            String extension = originalFileName != null ? originalFileName.substring(originalFileName.lastIndexOf("."))
                    : ".png";
            String uniqueFileName = UUID.randomUUID().toString() + extension;

            // Save file
            Path filePath = uploadPath.resolve(uniqueFileName);
            log.info("Saving file to: {}", filePath);
            file.transferTo(filePath.toFile());

            log.info("Icon uploaded successfully: {}", uniqueFileName);

            // Return response with filename
            Map<String, Object> response = new HashMap<>();
            response.put("filename", uniqueFileName);
            response.put("originalName", originalFileName);
            response.put("size", file.getSize());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IOException e) {
            log.error("Error uploading file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to upload file: " + e.getMessage()));
        }
    }

    private PaymentMethodDTO mapToDTO(PaymentMethod pm) {
        return new PaymentMethodDTO(
                pm.getId(),
                pm.getName(),
                pm.getPaymentMethodCode() != null
                        ? pm.getPaymentMethodCode().name()
                        : null,
                pm.isActive(),
                pm.isEnabled(),
                pm.getLastHeartbeat(),
                pm.getHostname(),
                pm.getHealthEndpoint(),
                pm.getPaymentEndpoint(),
                pm.getCreatedAt(),
                pm.getIconPath());
    }

    static class ErrorResponse {
        public String message;

        public ErrorResponse(String message) {
            this.message = message;
        }
    }
}
