package com.example.PaymentServiceProviderSEP.repository;

import com.example.PaymentServiceProviderSEP.model.PaymentMethod;
import com.example.PaymentServiceProviderSEP.model.PaymentMethodCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
    boolean existsByName(String name);

    Optional<PaymentMethod> findByName(String name);
    
    Optional<PaymentMethod> findByPaymentMethodCode(PaymentMethodCode paymentMethodCode);

    @Query("""
    SELECT pm
    FROM PaymentMethod pm
    WHERE pm.enabled = true
      AND pm.paymentMethodCode = :code
    ORDER BY
        CASE WHEN pm.lastHeartbeat IS NULL THEN 0 ELSE 1 END,
        pm.lastHeartbeat ASC
""")
    Optional<PaymentMethod> findNextForHeartbeat(
            @Param("code") PaymentMethodCode code
    );


    @Query("""
                SELECT pm
                FROM PaymentMethod pm
                WHERE (pm.active = true AND pm.paymentMethodCode='CUSTOM' OR pm.paymentMethodCode <> 'CUSTOM')
                  AND pm.id NOT IN (
                      SELECT s.paymentMethod.id
                      FROM MerchantPaymentMethodSubscription s
                      WHERE s.merchant.id = :merchantId
                  )
            """)
    List<PaymentMethod> findAvailableForMerchant(@Param("merchantId") Long merchantId);
}
