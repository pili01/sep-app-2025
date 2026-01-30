package com.example.PaymentServiceProviderSEP.service;

import com.example.PaymentServiceProviderSEP.dto.subscription.SubscriptionRequestDTO;
import com.example.PaymentServiceProviderSEP.dto.subscription.SubscriptionResponseDTO;
import com.example.PaymentServiceProviderSEP.model.Merchant;
import com.example.PaymentServiceProviderSEP.model.MerchantPaymentMethodSubscription;
import com.example.PaymentServiceProviderSEP.model.PaymentMethod;
import com.example.PaymentServiceProviderSEP.model.PaymentMethodCode;
import com.example.PaymentServiceProviderSEP.repository.MerchantPaymentMethodSubscriptionRepository;
import com.example.PaymentServiceProviderSEP.repository.MerchantRepository;
import com.example.PaymentServiceProviderSEP.repository.PaymentMethodRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MerchantPaymentMethodSubscriptionService {

    private final MerchantPaymentMethodSubscriptionRepository subscriptionRepository;
    private final MerchantRepository merchantRepository;
    private final HttpsClientService httpsClientService;
    private final PaymentMethodRepository paymentMethodRepository;

    @Transactional
    public List<SubscriptionResponseDTO> getSubscriptionsByMerchantId(Long merchantId) {
        return subscriptionRepository.findByMerchantId(merchantId).stream()
                .map(this::convertToDto)
                .toList();
    }

    @Transactional
    public MerchantPaymentMethodSubscription getSubscriptionById(Long id) {
        return subscriptionRepository.findByIdWithPaymentMethod(id)
                .orElseThrow(() -> new EntityNotFoundException("Subscription with id " + id + " not found"));
    }

    public List<MerchantPaymentMethodSubscription> getActiveSubscriptionsByMerchantId(Long merchantId) {
        return subscriptionRepository.findByMerchantIdAndEnabledTrue(merchantId);
    }

    @Transactional
    public List<PaymentMethod> getAvailablePaymentMethodsForMerchant(Long merchantId) {
        return paymentMethodRepository.findAvailableForMerchant(merchantId);
    }

    @Transactional
    public SubscriptionResponseDTO createSubscription(Long merchantId, SubscriptionRequestDTO subscriptionRequestDTO) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new EntityNotFoundException("Merchant with id " + merchantId + " not found"));

        PaymentMethod paymentMethod = paymentMethodRepository.findById(subscriptionRequestDTO.getPaymentMethodId())
                .orElseThrow(() -> new EntityNotFoundException("Payment method with id " + subscriptionRequestDTO.getPaymentMethodId() + " not found"));

        // Provera da li već postoji pretplata za ovaj servis plaćanja
        if (subscriptionRepository.existsByMerchantIdAndPaymentMethodId(merchantId, paymentMethod.getId())){
            throw new IllegalArgumentException("Subscription for payment method: " + paymentMethod.getPaymentMethodCode() + "-" + paymentMethod.getName()  + " already exists for this merchant");
        }

        if (paymentMethod.getPaymentMethodCode() == PaymentMethodCode.BANK_CARD) {
            String merchantIdFromBank = httpsClientService.getMerchantIdFromBankForAccountNumber(subscriptionRequestDTO.getMerchantAccountNumber());
            merchant.setMerchantIdFromBank(merchantIdFromBank);
        }

        MerchantPaymentMethodSubscription subscription = new MerchantPaymentMethodSubscription();
        subscription.setMerchant(merchant);
        subscription.setPaymentMethod(paymentMethod);
        subscription.setEnabled(true);
        subscription.setConfigJson(subscriptionRequestDTO.getConfigJson());

        subscriptionRepository.save(subscription);

        return convertToDto(subscription);
    }

    @Transactional
    public SubscriptionResponseDTO updateSubscription(Long subscriptionId, Boolean enabled, String configJson) {
        MerchantPaymentMethodSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new EntityNotFoundException("Subscription with id " + subscriptionId + " not found"));

        Long merchantId = subscription.getMerchant().getId();

        if (enabled != null && !enabled && subscription.getEnabled()) {
            long activeCount = subscriptionRepository.countByMerchantIdAndEnabledTrue(merchantId);
            if (activeCount <= 1) {
                throw new IllegalStateException("Cannot disable the last active payment method. At least one payment method must remain active.");
            }
        }

        if (enabled != null) {
            subscription.setEnabled(enabled);
        }
        if (configJson != null && !configJson.isBlank()) {
            subscription.setConfigJson(configJson);
        }

        subscriptionRepository.save(subscription);

        return convertToDto(subscription);
    }

    @Transactional
    public void deleteSubscription(Long subscriptionId) {
        MerchantPaymentMethodSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new EntityNotFoundException("Subscription with id " + subscriptionId + " not found"));

        Long merchantId = subscription.getMerchant().getId();

        // Validacija: Ne dozvoljava brisanje ako je to poslednji aktivan način plaćanja
        if (subscription.getEnabled()) {
            long activeCount = subscriptionRepository.countByMerchantIdAndEnabledTrue(merchantId);
            if (activeCount <= 1) {
                throw new IllegalStateException("Cannot delete the last active payment method. At least one payment method must remain active.");
            }
        }

        subscriptionRepository.delete(subscription);
    }

    private SubscriptionResponseDTO convertToDto(MerchantPaymentMethodSubscription entity) {
        return new SubscriptionResponseDTO(
                entity.getId(),
                entity.getMerchant().getId(),
                entity.getPaymentMethod().getId(),
                entity.getPaymentMethod().getName(),
                entity.getPaymentMethod().getPaymentMethodCode(),
                entity.getEnabled(),
                entity.getPaymentMethod().isActive(),
                entity.getConfigJson(),
                entity.getCreatedAt(),
                entity.getPaymentMethod().getIconPath()
        );
    }
}

