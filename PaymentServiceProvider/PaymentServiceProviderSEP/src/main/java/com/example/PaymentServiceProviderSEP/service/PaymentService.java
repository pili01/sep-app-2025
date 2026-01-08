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
    private final MerchantPaymentMethodSubscriptionRepository subscriptionRepository;
    private final CryptoService cryptoService;
    private final BankClientService bankClientService;
    private final ConfigProperties configProperties;
    private final TransactionService transactionService;
    private final MerchantPaymentMethodSubscriptionService merchantPaymentMethodSubscriptionService;
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
    public PaymentInitResponseDTO requestPaymentParametersFromBank(Map<String, String> requestMap) {
        Transaction transaction = transactionService.getTransactionById(Long.parseLong(requestMap.get("transactionId")));
        if (transaction == null) {
            throw new RuntimeException("Invalid transaction ID");
        }
        Merchant merchant = merchantRepository.findByMerchantId(transaction.getMerchantId())
                .orElseThrow(() -> new RuntimeException("Invalid merchant credentials"));

        if (merchant.getStatus() != MerchantStatus.ACTIVE) {
            throw new RuntimeException("Merchant is not active");
        }
        if (merchantPaymentMethodSubscriptionService.getActiveSubscriptionsByMerchantId(merchant.getId()).stream().noneMatch(as -> as.getPaymentMethodCode().toString().equals(requestMap.get("paymentMethodCode")) && as.getEnabled() != null && as.getEnabled())) {
            throw new RuntimeException("Merchant does not have active " + requestMap.get("paymentMethodCode") + " subscription");
        }
        transaction.setPaymentMethod(PaymentMethodCode.valueOf(requestMap.get("paymentMethodCode")));
        transaction.setStatus(TransactionStatus.PENDING);
        transactionService.createTransaction(transaction);

        // Create payment transaction in Bank
        try {
            Map<String, Object> bankResponse = bankClientService.createPaymentTransaction(
                    transaction.getMerchantIdFromBank(),
                    transaction.getAmount(),
                    transaction.getCurrency(),
                    transaction.getSTAN(),
                    transaction.getPspTimestamp()
            );

            return new PaymentInitResponseDTO(
                    bankResponse.get("paymentUrl").toString(),
                    bankResponse.get("paymentId").toString(),
                    "Payment initiated successfully"
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize payment: " + e.getMessage(), e);
        }
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
        } else {
            baseUrl = merchant.getErrorUrl();
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
}
