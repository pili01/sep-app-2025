package com.example.WebShopSEP.repository;

import com.example.WebShopSEP.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    boolean existsByTransactionId(String transactionId);
    Optional<Transaction> findByTransactionId(String transactionId);
}
