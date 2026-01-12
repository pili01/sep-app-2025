package com.example.PaymentServiceProviderSEP.service;

import com.example.PaymentServiceProviderSEP.config.ConfigProperties;
import com.example.PaymentServiceProviderSEP.dto.payment.PaymentInitRequestDTO;
import com.example.PaymentServiceProviderSEP.dto.payment.PaymentInitResponseDTO;
import com.example.PaymentServiceProviderSEP.dto.payment.PaymentStatusDTO;
import com.example.PaymentServiceProviderSEP.model.*;
import com.example.PaymentServiceProviderSEP.repository.MerchantPaymentMethodSubscriptionRepository;
import com.example.PaymentServiceProviderSEP.repository.MerchantRepository;
import com.example.PaymentServiceProviderSEP.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final MerchantRepository merchantRepository;
    private final CryptoService cryptoService;
    private final ConfigProperties configProperties;
    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;

    @Transactional
    public Map<String, String> initializePayment(PaymentInitRequestDTO request) {
        Merchant merchant = merchantRepository.findByMerchantId(request.getMerchantId())
                .orElseThrow(() -> new RuntimeException("Invalid merchant credentials"));

        if (merchant.getStatus() != MerchantStatus.ACTIVE) {
            throw new RuntimeException("Merchant is not active");
        }

        String decryptedPassword = cryptoService.decrypt(merchant.getMerchantPassword());
        if (!decryptedPassword.equals(request.getMerchantPassword())) {
            throw new RuntimeException("Invalid merchant credentials");
        }

        Transaction transaction = new Transaction(merchant.getMerchantId(), request.getAmount(), request.getCurrency(), merchant.getMerchantIdFromBank(), request.getMerchantOrderId());
        Transaction createdTransaction = transactionService.createTransaction(transaction);
        if (createdTransaction == null) {
            throw new RuntimeException("Failed to create transaction");
        }

        String redirectionUrl = configProperties.getFrontendBaseUrl() + "/payment/" + merchant.getId() + "?transactionId=" + createdTransaction.getId();

        return Map.of(
                "redirectionUrl", redirectionUrl,
                "message", "Payment initialized successfully"
        );
    }

    @Transactional
    public String processPaymentStatus(PaymentStatusDTO statusDTO) {
        // nadjem transakciju po stanu da li to moze tako
        Optional<Transaction> transactionOpt = transactionRepository.findBySTAN(statusDTO.getStan());
        if (transactionOpt.isEmpty()) {
            throw new RuntimeException("Transaction not found for STAN: " + statusDTO.getStan());
        }

        Transaction transaction = transactionOpt.get();
        transaction.setGlobalTransactionId(statusDTO.getGlobalTransactionId());

        try {
            LocalDateTime acquirerTimestamp = LocalDateTime.parse(statusDTO.getAcquirerTimestamp());
            transaction.setAcquirerTimestamp(acquirerTimestamp);
        } catch (Exception e) {
            throw new RuntimeException("Invalid acquirer timestamp format: " + statusDTO.getAcquirerTimestamp(), e);
        }

        if ("SUCCESS".equalsIgnoreCase(statusDTO.getStatus())) {
            transaction.setStatus(TransactionStatus.COMPLETED);
        } else if ("FAILED".equalsIgnoreCase(statusDTO.getStatus())) {
            transaction.setStatus(TransactionStatus.FAILED);
        } else if ("ERROR".equalsIgnoreCase(statusDTO.getStatus())) {
            transaction.setStatus(TransactionStatus.ERROR); // ERROR je poseban status
        } else {
            throw new RuntimeException("Invalid status: " + statusDTO.getStatus());
        }
        transactionRepository.save(transaction);

        Merchant merchant = merchantRepository.findByMerchantId(transaction.getMerchantId())
                .orElseThrow(() -> new RuntimeException("Merchant not found"));

        // Određujem baseUrl na osnovu statusa transakcije
        String baseUrl;
        if ("SUCCESS".equalsIgnoreCase(statusDTO.getStatus())) {
            baseUrl = merchant.getSuccessUrl();
        } else if ("FAILED".equalsIgnoreCase(statusDTO.getStatus())) {
            baseUrl = merchant.getFailedUrl();
        } else if ("ERROR".equalsIgnoreCase(statusDTO.getStatus())) {
            baseUrl = merchant.getErrorUrl();
        } else {
            baseUrl = merchant.getErrorUrl(); // Fallback na errorUrl za nepoznate statuse
        }

        // Konstruišem redirectUrl kao baseUrl + transactionId (merchantOrderId)
        // transactionId iz PSP transakcije je zapravo merchantOrderId iz WebShop-a
        // Koristim query parametar za transactionId
        String separator = baseUrl.contains("?") ? "&" : "?";
        String redirectUrl = baseUrl + separator + "transactionId=" + transaction.getTransactionId();

        return redirectUrl;
    }

    public TransactionStatus getTransactionStatusByTransactionId(String transactionId) {
        Optional<Transaction> transactionOpt = transactionRepository.findByTransactionId(transactionId);
        if (transactionOpt.isEmpty()) {
            throw new RuntimeException("Transaction not found: " + transactionId);
        }
        return transactionOpt.get().getStatus();
    }

    public Transaction getTransactionByTransactionId(String transactionId) {
        Optional<Transaction> transactionOpt = transactionRepository.findByTransactionId(transactionId);
        if (transactionOpt.isEmpty()) {
            throw new RuntimeException("Transaction not found: " + transactionId);
        }
        return transactionOpt.get();
    }
}
