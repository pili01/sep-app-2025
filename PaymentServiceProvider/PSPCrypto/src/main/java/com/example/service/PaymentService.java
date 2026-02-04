package com.example.service;

import com.example.dto.CryptoConfig;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final WebhookService webhookService;
    private final BitcoinClient bitcoinClient;
    private final com.example.config.ConfigProperties configProperties;

    @Transactional
    public Map<String, String> initiatePayment(PaymentRequest request) {

        String transactionId = String.valueOf(request.getTransactionId());
        BigDecimal amount = BigDecimal.valueOf(request.getAmount());


        if (transactionRepository.findByPspTransactionId(transactionId).isPresent()) {
            throw new IllegalArgumentException(
                    "Transaction can be initialized only one time: " + transactionId);
        }


        CryptoConfig cryptoConfig = bitcoinClient.parseConfig(request.getMerchantConfig());
        

        boolean hasBlockCypherToken = cryptoConfig.getBlockcypherApiToken() != null;
        boolean hasWalletAddress = cryptoConfig.getWalletAddress() != null && !cryptoConfig.getWalletAddress().trim().isEmpty();
        
        if (!hasBlockCypherToken && !hasWalletAddress) {
            throw new IllegalArgumentException(
                    "Either provide 'blockcypherApiToken' (can be empty string '' for testnet) to generate new addresses via BlockCypher API, " +
                    "or provide 'walletAddress' for static address (backward compatibility).");
        }


        BigDecimal bitcoinAmount = bitcoinClient.convertToBitcoin(
                amount,
                request.getCurrency()
        );
        



        String bitcoinAddress = bitcoinClient.generateBitcoinAddress(cryptoConfig);
        


        Transaction transaction = new Transaction();
        transaction.setPspTransactionId(transactionId);
        transaction.setAmount(amount);
        transaction.setCurrency(request.getCurrency());
        transaction.setBitcoinAmount(bitcoinAmount);
        transaction.setBitcoinAddress(bitcoinAddress);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setWebhookUrl(request.getWebhookUrl());
        transaction.setSuccessUrl(request.getSuccessUrl());
        transaction.setFailedUrl(request.getFailedUrl());
        transaction.setErrorUrl(request.getErrorUrl());
        transaction.setRequiredConfirmations(
                cryptoConfig.getRequiredConfirmations() != null ? 
                cryptoConfig.getRequiredConfirmations() : 1
        );
        transaction.setConfirmations(0);
        transaction.setPspNotified(false);
        
        transactionRepository.save(transaction);
        
        
        String paymentUrl = configProperties.getPspFrontendUrl() + "/payment/crypto/" + transaction.getPspTransactionId();


        return Map.of(
                "paymentId", transaction.getPspTransactionId(),
                "paymentUrl", paymentUrl
        );
    }

    @Transactional(readOnly = true)
    public TransactionStatusResponse getTransactionStatus(String transactionId) {

        Transaction transaction = transactionRepository.findByPspTransactionId(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));


        TransactionStatusResponse response = TransactionStatusResponse.builder()
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
                .successUrl(transaction.getSuccessUrl())
                .failedUrl(transaction.getFailedUrl())
                .errorUrl(transaction.getErrorUrl())
                .build();

        return response;
    }
}
