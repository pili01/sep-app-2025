package com.example.PaymentServiceProviderSEP.service;

import com.example.PaymentServiceProviderSEP.config.ConfigProperties;
import com.example.PaymentServiceProviderSEP.dto.payment.PaymentInitRequestDTO;
import com.example.PaymentServiceProviderSEP.dto.payment.PaymentInitResponseDTO;
import com.example.PaymentServiceProviderSEP.model.Merchant;
import com.example.PaymentServiceProviderSEP.model.MerchantStatus;
import com.example.PaymentServiceProviderSEP.model.PaymentMethodCode;
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

        String redirectionUrl = configProperties.getFrontendBaseUrl() + "/payment/" + merchant.getId();

        return Map.of(
                "redirectionUrl", redirectionUrl,
                "message", "Payment initialized successfully"
        );
    }

    @Transactional
    public PaymentInitResponseDTO requestPaymentParametersFromBank(PaymentInitRequestDTO request) {
        Merchant merchant = merchantRepository.findByMerchantId(request.getMerchantId())
                .orElseThrow(() -> new RuntimeException("Invalid merchant credentials"));

        if (merchant.getStatus() != MerchantStatus.ACTIVE) {
            throw new RuntimeException("Merchant is not active");
        }

        // Verify merchant password
        String decryptedPassword = cryptoService.decrypt(merchant.getMerchantPassword());
        if (!decryptedPassword.equals(request.getMerchantPassword())) {
            throw new RuntimeException("Invalid merchant credentials");
        }

        // Check if merchant has active bank card subscription
        boolean hasBankCardSubscription = subscriptionRepository
                .findByMerchantIdAndPaymentMethodCode(merchant.getId(), PaymentMethodCode.BANK_CARD)
                .filter(sub -> sub.getEnabled() != null && sub.getEnabled())
                .isPresent();

        if (!hasBankCardSubscription) {
            throw new RuntimeException("Merchant does not have active bank card subscription");
        }

        // Generate payment ID
        String paymentId = UUID.randomUUID().toString();

        // Create payment transaction in Bank
        try {
            String bankPaymentId = bankClientService.createPaymentTransaction(
                    request.getAmount(),
                    request.getCurrency(),
                    merchant.getName()
            );

            // Return payment URL for frontend
            String paymentUrl = "https://localhost:4202/payment/" + paymentId + "?bankPaymentId=" + bankPaymentId;

            return new PaymentInitResponseDTO(
                    paymentUrl,
                    paymentId,
                    "Payment initialized successfully"
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize payment: " + e.getMessage(), e);
        }
    }
}
