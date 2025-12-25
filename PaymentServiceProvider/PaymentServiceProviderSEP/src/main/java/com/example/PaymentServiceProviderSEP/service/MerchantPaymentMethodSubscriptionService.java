package com.example.PaymentServiceProviderSEP.service;

import com.example.PaymentServiceProviderSEP.model.Merchant;
import com.example.PaymentServiceProviderSEP.model.MerchantPaymentMethodSubscription;
import com.example.PaymentServiceProviderSEP.model.PaymentMethodCode;
import com.example.PaymentServiceProviderSEP.repository.MerchantPaymentMethodSubscriptionRepository;
import com.example.PaymentServiceProviderSEP.repository.MerchantRepository;
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

    public List<MerchantPaymentMethodSubscription> getSubscriptionsByMerchantId(Long merchantId) {
        return subscriptionRepository.findByMerchantId(merchantId);
    }

    public List<MerchantPaymentMethodSubscription> getActiveSubscriptionsByMerchantId(Long merchantId) {
        return subscriptionRepository.findByMerchantIdAndEnabledTrue(merchantId);
    }

    @Transactional
    public MerchantPaymentMethodSubscription createSubscription(Long merchantId, PaymentMethodCode paymentMethodCode, String configJson) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new EntityNotFoundException("Merchant with id " + merchantId + " not found"));

        // Provera da li već postoji pretplata za ovaj način plaćanja
        if (subscriptionRepository.existsByMerchantIdAndPaymentMethodCode(merchantId, paymentMethodCode)) {
            throw new IllegalArgumentException("Subscription for payment method " + paymentMethodCode + " already exists for this merchant");
        }

        MerchantPaymentMethodSubscription subscription = new MerchantPaymentMethodSubscription();
        subscription.setMerchant(merchant);
        subscription.setPaymentMethodCode(paymentMethodCode);
        subscription.setEnabled(true);
        subscription.setConfigJson(configJson);

        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public MerchantPaymentMethodSubscription updateSubscription(Long subscriptionId, Boolean enabled, String configJson) {
        MerchantPaymentMethodSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new EntityNotFoundException("Subscription with id " + subscriptionId + " not found"));

        Long merchantId = subscription.getMerchant().getId();

        // Validacija: Ne dozvoljava onemogućavanje ako je to poslednji aktivan način plaćanja
        if (enabled != null && !enabled && subscription.getEnabled()) {
            long activeCount = subscriptionRepository.countByMerchantIdAndEnabledTrue(merchantId);
            if (activeCount <= 1) {
                throw new IllegalStateException("Cannot disable the last active payment method. At least one payment method must remain active.");
            }
        }

        if (enabled != null) {
            subscription.setEnabled(enabled);
        }
        if (configJson != null) {
            subscription.setConfigJson(configJson);
        }

        return subscriptionRepository.save(subscription);
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

    public MerchantPaymentMethodSubscription getSubscriptionById(Long subscriptionId) {
        return subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new EntityNotFoundException("Subscription with id " + subscriptionId + " not found"));
    }
}

