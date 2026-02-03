package com.example.service;

import com.example.dto.CheckStatusRequest;
import com.example.dto.CheckStatusResponse;
import com.example.model.Transaction;
import com.example.model.TransactionStatus;
import com.example.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentVerificationService {

    private final TransactionRepository transactionRepository;
    private final BitcoinClient bitcoinClient;

    @Transactional
    public CheckStatusResponse verifyPaymentStatus(CheckStatusRequest request) {
        log.info("Verifying payment status for transaction: {}", request.getTransactionId());

        // Pronađi transakciju u bazi
        Transaction transaction = transactionRepository.findByPspTransactionId(request.getTransactionId())
                .orElseThrow(
                        () -> new IllegalArgumentException("Transaction not found: " + request.getTransactionId()));

        // Označi da je PSP notifikovan
        transaction.setPspNotified(true);
        transactionRepository.save(transaction);

        // TODO: Implementirati blockchain proveru
        // Proveri da li je transakcija completed i ima dovoljno potvrda

        boolean verified = transaction.getStatus() == TransactionStatus.COMPLETED &&
                transaction.getConfirmations() != null &&
                transaction.getConfirmations() >= transaction.getRequiredConfirmations();

        return CheckStatusResponse.builder()
                .transactionId(request.getTransactionId())
                .status(transaction.getStatus().name())
                .verified(verified)
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .message(verified ? "Payment verified" : "Payment not confirmed")
                .build();
    }
}
