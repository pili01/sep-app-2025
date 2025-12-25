package com.example.PaymentServiceProviderSEP.repository;

import com.example.PaymentServiceProviderSEP.model.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, Long> {
    Optional<Merchant> findByMerchantId(String merchantId);
}

