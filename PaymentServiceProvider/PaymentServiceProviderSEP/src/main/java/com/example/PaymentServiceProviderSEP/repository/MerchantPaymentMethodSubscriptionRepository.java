package com.example.PaymentServiceProviderSEP.repository;

import com.example.PaymentServiceProviderSEP.model.MerchantPaymentMethodSubscription;
import com.example.PaymentServiceProviderSEP.model.PaymentMethodCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantPaymentMethodSubscriptionRepository extends JpaRepository<MerchantPaymentMethodSubscription, Long> {

    List<MerchantPaymentMethodSubscription> findByMerchantId(Long merchantId);

    List<MerchantPaymentMethodSubscription> findByMerchantIdAndEnabledTrue(Long merchantId);

    long countByMerchantIdAndEnabledTrue(Long merchantId);

    Optional<MerchantPaymentMethodSubscription> findByMerchantIdAndPaymentMethodCode(Long merchantId, PaymentMethodCode paymentMethodCode);

    boolean existsByMerchantIdAndPaymentMethodCode(Long merchantId, PaymentMethodCode paymentMethodCode);
}

