package com.example.service;

import com.example.dto.PaymentRequest;
import com.example.dto.TransactionStatusResponse;
import com.example.model.Transaction;
import com.example.model.TransactionStatus;
import com.example.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final WebhookService webhookService;
    private final BitcoinClient bitcoinClient;

    @Value("${server.port}")
    private String serverPort;

    @Transactional
    public Map<String, String> initiatePayment(PaymentRequest request) {
        log.info("Initiating crypto payment for transaction: {}", request.getTransactionId());

        // Check if transaction already exists
        if (transactionRepository.findByPspTransactionId(request.getTransactionId()).isPresent()) {
            throw new IllegalArgumentException(
                    "Transaction can be initialized only one time: " + request.getTransactionId());
        }

        // TODO: Implementirati crypto payment logiku
        // 1. Parsira crypto config
        // 2. Konvertuje fiat → BTC
        // 3. Generiše Bitcoin adresu
        // 4. Kreira transakciju u bazi
        // 5. Vraća payment URL

        throw new UnsupportedOperationException("Crypto payment not yet implemented");
    }

    @Transactional(readOnly = true)
    public TransactionStatusResponse getTransactionStatus(String transactionId) {
        log.info("Checking status for transaction: {}", transactionId);

        Transaction transaction = transactionRepository.findByPspTransactionId(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        return TransactionStatusResponse.builder()
                .transactionId(transaction.getPspTransactionId())
                .status(transaction.getStatus().name())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .bitcoinAmount(transaction.getBitcoinAmount())
                .bitcoinAddress(transaction.getBitcoinAddress())
                .confirmations(transaction.getConfirmations())
                .createdAt(transaction.getCreatedAt())
                .completedAt(transaction.getCompletedAt())
                .errorMessage(transaction.getErrorMessage())
                .build();
    }
}
