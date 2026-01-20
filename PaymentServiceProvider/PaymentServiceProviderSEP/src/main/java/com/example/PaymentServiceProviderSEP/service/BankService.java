package com.example.PaymentServiceProviderSEP.service;

import com.example.PaymentServiceProviderSEP.config.ConfigProperties;
import com.example.PaymentServiceProviderSEP.dto.payment.PaymentInitResponseDTO;
import com.example.PaymentServiceProviderSEP.model.*;
import com.example.PaymentServiceProviderSEP.repository.MerchantPaymentMethodSubscriptionRepository;
import com.example.PaymentServiceProviderSEP.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class BankService {
    private final BankClientService bankClientService;
    private final PublicClientService publicClientService;
    private final MerchantPaymentMethodSubscriptionService merchantPaymentMethodSubscriptionService;
    private final MerchantPaymentMethodSubscriptionRepository subscriptionRepository;
    private final TransactionService transactionService;
    private final MerchantRepository merchantRepository;
    private final ConfigProperties configProperties;

    @Transactional
    public PaymentInitResponseDTO requestPaymentParametersFromBank(MerchantPaymentMethodSubscription subscription, Long transactionId) {
        Transaction transaction = transactionService.getTransactionById(transactionId);
        PaymentMethod paymentMethod = subscription.getPaymentMethod();
        PaymentMethodCode  paymentMethodCode = paymentMethod.getPaymentMethodCode();

        if (transaction == null) {
            throw new RuntimeException("Invalid transaction ID");
        }
        Merchant merchant = merchantRepository.findByMerchantId(transaction.getMerchantId())
                .orElseThrow(() -> new RuntimeException("Invalid merchant credentials"));

        if (merchant.getStatus() != MerchantStatus.ACTIVE) {
            throw new RuntimeException("Merchant is not active");
        }

        if(merchant.getId() != subscription.getMerchant().getId() || !subscription.getPaymentMethod().isActive() ||
         !subscription.getEnabled()){
            throw new RuntimeException("Merchant does not have active " + subscription.getPaymentMethod().getPaymentMethodCode() + " subscription");
        }

        transaction.setPaymentMethod(PaymentMethodCode.valueOf(paymentMethodCode.name()));
        transaction.setStatus(TransactionStatus.PENDING);
        transactionService.createTransaction(transaction);

        // Create payment transaction in Bank
        try {
            Map<String, Object> bankResponse = null;
            if (paymentMethodCode == PaymentMethodCode.BANK_CARD) {
                String url = configProperties.getExchangeRateApiUrl() + transaction.getCurrency() + "/RSD/" + transaction.getAmount();
                Double dinAmount = publicClientService.getConversationResult(url);
                bankResponse = bankClientService.createPaymentTransaction(
                        transaction.getMerchantIdFromBank(),
                        dinAmount,
                        "RSD",
                        transaction.getSTAN(),
                        transaction.getPspTimestamp()
                );
            } else if (paymentMethodCode == PaymentMethodCode.BANK_QR) {
                String url = configProperties.getExchangeRateApiUrl() + transaction.getCurrency() + "/RSD/" + transaction.getAmount();
                Double dinAmount = publicClientService.getConversationResult(url);
                bankResponse = bankClientService.generateQrPaymentTransaction(
                        transaction.getMerchantIdFromBank(),
                        dinAmount,
                        "RSD",
                        transaction.getSTAN(),
                        transaction.getPspTimestamp()
                );
            } else {
                throw new RuntimeException("Unsupported payment method code for bank request");
            }

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
