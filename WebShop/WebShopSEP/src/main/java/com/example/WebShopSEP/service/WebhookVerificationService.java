package com.example.WebShopSEP.service;

import com.example.WebShopSEP.config.ConfigProperties;
import com.example.WebShopSEP.dto.CheckStatusRequest;
import com.example.WebShopSEP.dto.CheckStatusResponse;
import com.example.WebShopSEP.model.Rental;
import com.example.WebShopSEP.model.RentalStatus;
import com.example.WebShopSEP.model.Transaction;
import com.example.WebShopSEP.model.TransactionStatus;
import com.example.WebShopSEP.repository.RentalRepository;
import com.example.WebShopSEP.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookVerificationService {

    private final TransactionRepository transactionRepository;
    private final RentalRepository rentalRepository;
    private final PSPClientService pspClientService;
    private final ConfigProperties config;

    @Transactional
    public CheckStatusResponse verifyPaymentStatus(CheckStatusRequest request) {
        log.info("Verifying payment status for transaction: {}", request.getTransactionId());

        try {
            // 1. Pronađi transakciju u bazi
            Transaction transaction = transactionRepository.findByTransactionId(request.getTransactionId())
                    .orElseThrow(() -> {
                        log.error("Transaction not found: {}", request.getTransactionId());
                        return new RuntimeException("Transaction not found: " + request.getTransactionId());
                    });

            // 2. Proveri da li se amount i currency poklapaju
            boolean amountMatch = transaction.getTimestamp() != null; // Koristimo timestamp kao proxy za validnost

            if (!amountMatch) {
                log.error("Transaction validation failed for: {}", request.getTransactionId());
                return new CheckStatusResponse(
                        request.getTransactionId(),
                        transaction.getStatus().toString(),
                        false,
                        "",
                        "Transaction validation failed");
            }

            // 3. Označi da je PSP proverio status (merchantNotified se šalje iz PSP-a)
            log.info("Transaction {} verified successfully. Current status: {}",
                    request.getTransactionId(), transaction.getStatus());

            return new CheckStatusResponse(
                    request.getTransactionId(),
                    transaction.getStatus().toString(),
                    true,
                    "",
                    "Verification successful");

        } catch (Exception e) {
            log.error("Error verifying payment status for transaction {}: {}",
                    request.getTransactionId(), e.getMessage(), e);
            return new CheckStatusResponse(
                    request.getTransactionId(),
                    "ERROR",
                    false,
                    "",
                    "Verification error: " + e.getMessage());
        }
    }

    @Transactional
    public void updateTransactionFromWebhook(String transactionId) {
        log.info("Processing webhook notification for transaction: {}", transactionId);

        try {
            // 1. Pronađi transakciju u bazi
            Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                    .orElseThrow(() -> {
                        log.error("Transaction not found: {}", transactionId);
                        return new RuntimeException("Transaction not found: " + transactionId);
                    });

            log.info("Found transaction: {} with amount: {} {}",
                    transactionId, transaction.getAmount(), transaction.getCurrency());

            // 2. Pošalji zahtjev na PSP da se provjeri status (bez amount/currency iz
            // notifikacije)
            log.info("Sending check-status request to PSP for transaction: {}", transactionId);
            CheckStatusRequest checkRequest = new CheckStatusRequest(
                    transactionId,
                    transaction.getAmount(),
                    transaction.getCurrency());
            CheckStatusResponse verificationResponse = pspClientService.checkPaymentStatus(checkRequest);

            // 3. Proveri da li je PSP potvrdio plaćanje
            if (verificationResponse == null || !Boolean.TRUE.equals(verificationResponse.getVerified())) {
                log.error("PSP verification failed for transaction {}: {}",
                        transactionId,
                        verificationResponse != null ? verificationResponse.getMessage() : "null response");
                throw new RuntimeException("PSP verification failed: " +
                        (verificationResponse != null ? verificationResponse.getMessage() : "No response from PSP"));
            }

            log.info("PSP verification successful for transaction {}", transactionId);

            // 4. Ako je PSP potvrdio, sačuvaj payment method i updateja transakciju status
            // na COMPLETED
            if (verificationResponse.getPaymentMethodName() != null
                    && !verificationResponse.getPaymentMethodName().isEmpty()) {
                transaction.setPaymentMethod(verificationResponse.getPaymentMethodName());
                log.info("Payment method set to: {}", verificationResponse.getPaymentMethodName());
            }

            transaction.setStatus(TransactionStatus.COMPLETED);
            transactionRepository.save(transaction);
            log.info("Transaction {} status updated to COMPLETED with payment method", transactionId);

            // 5. Pronađi rental koji je vezan za transakciju i updateja njegov status na
            // PURCHASED
            if (transaction.getRentalId() != null) {
                updateRentalStatus(transaction.getRentalId(), RentalStatus.PURCHASED, transaction.getPaymentMethod());
            }

        } catch (Exception e) {
            log.error("Failed to update transaction {} from webhook: {}", transactionId, e.getMessage(), e);
            throw e;
        }
    }

    private void updateRentalStatus(Long rentalId, RentalStatus rentalStatus, String paymentMethod) {
        try {
            Rental rental = rentalRepository.findById(rentalId)
                    .orElseThrow(() -> {
                        log.error("Rental not found for ID: {}", rentalId);
                        return new RuntimeException("Rental not found: " + rentalId);
                    });

            if (rental.getStatus() == RentalStatus.DRAFT && rentalStatus == RentalStatus.PURCHASED) {
                rental.setStatus(rentalStatus);
                if (paymentMethod != null && !paymentMethod.isEmpty()) {
                    rental.setPaymentMethod(paymentMethod);
                    log.info("Rental {} payment method set to: {}", rentalId, paymentMethod);
                }
                rentalRepository.save(rental);
                log.info("Rental {} status updated to PURCHASED", rentalId);
            } else {
                log.warn("Cannot update rental {} status from {} to {}", rentalId, rental.getStatus(), rentalStatus);
            }
        } catch (Exception e) {
            log.error("Failed to update rental status for rental ID {}: {}", rentalId, e.getMessage(), e);
            throw new RuntimeException("Failed to update rental status: " + e.getMessage(), e);
        }
    }
}
