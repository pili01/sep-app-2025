package com.example.PaymentServiceProviderSEP.repository;

import com.example.PaymentServiceProviderSEP.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findBySTAN(String stan);
    Optional<Transaction> findByTransactionId(String transactionId);
}
