package com.example.PaymentServiceProviderSEP.service;

import com.example.PaymentServiceProviderSEP.dto.CheckStatusRequest;
import com.example.PaymentServiceProviderSEP.model.PaymentMethod;
import com.example.PaymentServiceProviderSEP.model.Transaction;
import com.example.PaymentServiceProviderSEP.model.TransactionStatus;
import com.example.PaymentServiceProviderSEP.repository.PaymentMethodRepository;
import com.example.PaymentServiceProviderSEP.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookVerificationService {

    private final TransactionRepository transactionRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final HttpsClientService httpsClientService;
    private final MerchantWebhookService merchantWebhookService;

    public void verifyAndUpdateTransaction(String transactionId) {
        log.info("Verifying transaction from webhook: {}", transactionId);

        // 1. Pronađi transakciju u bazi
        Transaction transaction = transactionRepository.findById(Long.valueOf(transactionId))
                .orElseThrow(() -> {
                    log.error("Transaction not found: {}", transactionId);
                    return new RuntimeException("Transaction not found: " + transactionId);
                });

        // 2. Dobavi payment method na osnovu ID-ja
        if (transaction.getPaymentMethodId() == null) {
            log.error("Transaction {} has no payment method ID set", transactionId);
            throw new RuntimeException("Transaction has no payment method ID");
        }

        PaymentMethod paymentMethod = paymentMethodRepository
                .findById(transaction.getPaymentMethodId())
                .orElseThrow(() -> {
                    log.error("Payment method not found for ID: {}", transaction.getPaymentMethodId());
                    return new RuntimeException("Payment method not found for ID: " + transaction.getPaymentMethodId());
                });

        // 3. Kreiraj check-status request
        CheckStatusRequest checkRequest = new CheckStatusRequest(
                transactionId,
                transaction.getAmount(),
                transaction.getCurrency());

        try {
            // 4. Pošalji zahtev na payment method check-status endpoint
            String checkStatusUrl = paymentMethod.getHostname() + "/api/webhook/check-status";
            log.info("Sending check-status request to: {}", checkStatusUrl);
            var response = httpsClientService.checkPaymentStatus(checkStatusUrl, checkRequest);

            if (response == null) {
                log.error("Received null response from check-status endpoint");
                transaction.setStatus(TransactionStatus.ERROR);
                transactionRepository.save(transaction);
                merchantWebhookService.notifyMerchant(transaction);
                return;
            }

            log.info("Check-status response: verified={}, status={}",
                    response.getVerified(), response.getStatus());

            // 5. Update transakcije na osnovu verifikacije
            if (Boolean.TRUE.equals(response.getVerified())) {
                TransactionStatus newStatus = TransactionStatus.valueOf(response.getStatus());
                transaction.setStatus(newStatus);
                log.info("Transaction {} verified and updated to status: {}",
                        transactionId, newStatus);
            } else {
                transaction.setStatus(TransactionStatus.ERROR);
                log.warn("Transaction {} verification failed: {}",
                        transactionId, response.getMessage());
            }

            transactionRepository.save(transaction);
            merchantWebhookService.notifyMerchant(transaction);

        } catch (Exception e) {
            log.error("Failed to verify transaction {}: {}", transactionId, e.getMessage(), e);
            transaction.setStatus(TransactionStatus.ERROR);
            transactionRepository.save(transaction);
            merchantWebhookService.notifyMerchant(transaction);
        }
    }
}
