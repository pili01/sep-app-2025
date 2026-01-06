package com.example.PaymentServiceProviderSEP.service;

import com.example.PaymentServiceProviderSEP.config.ConfigProperties;
import com.example.PaymentServiceProviderSEP.dto.payment.PaymentInitRequestDTO;
import com.example.PaymentServiceProviderSEP.dto.payment.PaymentInitResponseDTO;
import com.example.PaymentServiceProviderSEP.model.*;
import com.example.PaymentServiceProviderSEP.repository.MerchantPaymentMethodSubscriptionRepository;
import com.example.PaymentServiceProviderSEP.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
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
}
