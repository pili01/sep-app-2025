package com.example.PaymentServiceProviderSEP.repository;

import com.example.PaymentServiceProviderSEP.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
