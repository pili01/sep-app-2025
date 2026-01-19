package com.example.PaymentServiceProviderSEP.repository;

import com.example.PaymentServiceProviderSEP.model.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
    boolean existsByName(String name);
    Optional<PaymentMethod> findByName(String name);
    Optional<PaymentMethod> findFirstByOrderByCheckIndexAscIdAsc();
}
