package com.example.WebShopSEP.repository;

import com.example.WebShopSEP.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    boolean existsByTransactionId(String transactionId);
}
