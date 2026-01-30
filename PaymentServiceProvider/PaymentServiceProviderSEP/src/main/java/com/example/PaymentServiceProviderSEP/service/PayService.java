package com.example.PaymentServiceProviderSEP.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.PaymentServiceProviderSEP.config.ConfigProperties;
import com.example.PaymentServiceProviderSEP.dto.payment.PaymentInitResponseDTO;
import com.example.PaymentServiceProviderSEP.model.Merchant;
import com.example.PaymentServiceProviderSEP.model.MerchantPaymentMethodSubscription;
import com.example.PaymentServiceProviderSEP.model.MerchantStatus;
import com.example.PaymentServiceProviderSEP.model.PaymentMethod;
import com.example.PaymentServiceProviderSEP.model.PaymentMethodCode;
import com.example.PaymentServiceProviderSEP.model.Transaction;
import com.example.PaymentServiceProviderSEP.model.TransactionStatus;
import com.example.PaymentServiceProviderSEP.repository.MerchantRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class PayService {
    private final HttpsClientService httpsClientService;
    private final PublicClientService publicClientService;
    private final TransactionService transactionService;
    private final MerchantRepository merchantRepository;
    private final ConfigProperties configProperties;

    @Transactional
    public PaymentInitResponseDTO requestPaymentParameters(MerchantPaymentMethodSubscription subscription,
            Long transactionId) {
        Transaction transaction = transactionService.getTransactionById(transactionId);
        PaymentMethod paymentMethod = subscription.getPaymentMethod();
        PaymentMethodCode paymentMethodCode = paymentMethod.getPaymentMethodCode();

        if (transaction == null) {
            throw new RuntimeException("Invalid transaction ID");
        }
        Merchant merchant = merchantRepository.findByMerchantId(transaction.getMerchantId())
                .orElseThrow(() -> new RuntimeException("Invalid merchant credentials"));

        if (merchant.getStatus() != MerchantStatus.ACTIVE) {
            throw new RuntimeException("Merchant is not active");
        }

        if (merchant.getId() != subscription.getMerchant().getId() || !subscription.getPaymentMethod().isActive() ||
                !subscription.getEnabled()) {
            throw new RuntimeException("Merchant does not have active "
                    + subscription.getPaymentMethod().getPaymentMethodCode() + " subscription");
        }

        transaction.setPaymentMethod(PaymentMethodCode.valueOf(paymentMethodCode.name()));
        transaction.setPaymentMethodId(paymentMethod.getId());
        transaction.setStatus(TransactionStatus.PENDING);
        transactionService.createTransaction(transaction);

        // Create payment transaction in Bank
        try {
            Map<String, Object> paymentServiceResponse = null;
            if (paymentMethodCode == PaymentMethodCode.BANK_CARD) {
                String url = configProperties.getExchangeRateApiUrl() + transaction.getCurrency() + "/RSD/"
                        + transaction.getAmount();
                Double dinAmount = publicClientService.getConversationResult(url);
                paymentServiceResponse = httpsClientService.createPaymentTransaction(
                        transaction.getMerchantIdFromBank(),
                        dinAmount,
                        "RSD",
                        transaction.getSTAN(),
                        transaction.getPspTimestamp());
            } else if (paymentMethodCode == PaymentMethodCode.BANK_QR) {
                String url = configProperties.getExchangeRateApiUrl() + transaction.getCurrency() + "/RSD/"
                        + transaction.getAmount();
                Double dinAmount = publicClientService.getConversationResult(url);
                paymentServiceResponse = httpsClientService.generateQrPaymentTransaction(
                        transaction.getMerchantIdFromBank(),
                        dinAmount,
                        "RSD",
                        transaction.getSTAN(),
                        transaction.getPspTimestamp());
            } else {
                paymentServiceResponse = httpsClientService.initializePayment(paymentMethod.getPaymentEndpoint(),
                        subscription.getConfigJson(), transaction.getId(), transaction.getAmount(),
                        transaction.getCurrency(), merchant.getSuccessUrl(), merchant.getFailedUrl(),
                        merchant.getErrorUrl());
            }

            return new PaymentInitResponseDTO(
                    paymentServiceResponse.get("paymentUrl").toString(),
                    paymentServiceResponse.get("paymentId").toString(),
                    "Payment initiated successfully");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize payment: " + e.getMessage(), e);
        }
    }
}
