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
        // Convert Long transactionId to String and Double amount to BigDecimal
        String transactionId = String.valueOf(request.getTransactionId());
        BigDecimal amount = BigDecimal.valueOf(request.getAmount());
        
        log.info("Initiating crypto payment for transaction: {}", transactionId);

        // Check if transaction already exists
        if (transactionRepository.findByPspTransactionId(transactionId).isPresent()) {
            throw new IllegalArgumentException(
                    "Transaction can be initialized only one time: " + transactionId);
        }

        // 1. Parsira crypto config iz merchant config JSON
        CryptoConfig cryptoConfig = bitcoinClient.parseConfig(request.getMerchantConfig());
        
        // Validacija: mora postojati bar jedna od opcija:
        // - blockcypherApiToken (za generisanje novih adresa) - može biti prazan string "" za testnet
        // - walletAddress (za statičku adresu - backward compatibility)
        // 
        // Ako koristiš BlockCypher API, walletAddress je opciono (samo za fallback ako API ne radi)
        boolean hasBlockCypherToken = cryptoConfig.getBlockcypherApiToken() != null;
        boolean hasWalletAddress = cryptoConfig.getWalletAddress() != null && !cryptoConfig.getWalletAddress().trim().isEmpty();
        
        if (!hasBlockCypherToken && !hasWalletAddress) {
            throw new IllegalArgumentException(
                    "Either provide 'blockcypherApiToken' (can be empty string '' for testnet) to generate new addresses via BlockCypher API, " +
                    "or provide 'walletAddress' for static address (backward compatibility).");
        }

        // 2. Konvertuje fiat → BTC
        BigDecimal bitcoinAmount = bitcoinClient.convertToBitcoin(
                amount,
                request.getCurrency()
        );
        
        log.info("Converted {} {} to {} BTC", amount, request.getCurrency(), bitcoinAmount);

        // 3. Generiše Bitcoin adresu (merchant-ova adresa)
        String bitcoinAddress = bitcoinClient.generateBitcoinAddress(cryptoConfig);
        
        log.info("Using Bitcoin address: {}", bitcoinAddress);

        // 4. Kreira transakciju u bazi
        Transaction transaction = new Transaction();
        transaction.setPspTransactionId(transactionId);
        transaction.setAmount(amount);
        transaction.setCurrency(request.getCurrency());
        transaction.setBitcoinAmount(bitcoinAmount);
        transaction.setBitcoinAddress(bitcoinAddress);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setMerchantConfig(request.getMerchantConfig());
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
        
        log.info("Transaction saved: ID={}, Status={}, BitcoinAmount={}, BitcoinAddress={}",
                transaction.getId(), transaction.getStatus(), bitcoinAmount, bitcoinAddress);

        // 5. Vraća payment URL (PSP frontend stranica za prikaz Bitcoin adrese)
        // PSP frontend će pozvati Crypto API da dobije podatke
        String paymentUrl = configProperties.getPspFrontendUrl() + "/payment/crypto/" + transaction.getPspTransactionId();
        
        log.info("Crypto payment initiated successfully. Transaction ID: {}, Payment URL: {}",
                transaction.getPspTransactionId(), paymentUrl);

        return Map.of(
                "paymentId", transaction.getPspTransactionId(),
                "paymentUrl", paymentUrl
        );
    }

    @Transactional(readOnly = true)
    public TransactionStatusResponse getTransactionStatus(String transactionId) {
        log.info("Checking status for transaction: {}", transactionId);

        Transaction transaction = transactionRepository.findByPspTransactionId(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        log.info("Transaction found: ID={}, BitcoinAmount={}, BitcoinAddress={}", 
                transaction.getId(), transaction.getBitcoinAmount(), transaction.getBitcoinAddress());

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
                .build();
        
        log.info("Response: BitcoinAmount={}", response.getBitcoinAmount());
        return response;
    }
}
