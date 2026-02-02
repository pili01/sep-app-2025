package com.example.PaymentServiceProviderSEP.service;

import com.example.PaymentServiceProviderSEP.dto.CheckStatusRequest;
import com.example.PaymentServiceProviderSEP.dto.CheckStatusResponse;
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
public class MerchantVerificationService {

    private final TransactionRepository transactionRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    public CheckStatusResponse verifyMerchantCheckStatus(CheckStatusRequest request) {
        log.info("Verifying merchant check-status for transaction: {}", request.getTransactionId());

        try {
            // Pronađi transakciju po transactionId
            Transaction transaction = transactionRepository.findByTransactionId(request.getTransactionId())
                    .orElseThrow(() -> {
                        log.error("Transaction not found: {}", request.getTransactionId());
                        return new RuntimeException("Transaction not found: " + request.getTransactionId());
                    });

            var paymentMethod = paymentMethodRepository.findById(transaction.getPaymentMethodId());
            // Označi da je merchant notifikovan
            transaction.setMerchantNotified(true);
            transactionRepository.save(transaction);

            log.info("Transaction {} marked as merchant notified. Status: {}",
                    request.getTransactionId(), transaction.getStatus());

            return new CheckStatusResponse(
                    request.getTransactionId(),
                    transaction.getStatus().toString(),
                    true,
                    paymentMethod.get().getName(),
                    "Verification successful",
                    transaction.getStatus());

        } catch (Exception e) {
            log.error("Error verifying merchant check-status for transaction {}: {}",
                    request.getTransactionId(), e.getMessage(), e);
            return new CheckStatusResponse(
                    request.getTransactionId(),
                    "ERROR",
                    false,
                    null,
                    "Verification error: " + e.getMessage(),
                    TransactionStatus.ERROR);
        }
    }
}
