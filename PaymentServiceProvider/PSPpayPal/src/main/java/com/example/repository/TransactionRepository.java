package com.example.repository;

import com.example.model.Transaction;
import com.example.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByPspTransactionId(String globalTransactionId);

    Optional<Transaction> findByGlobalTransactionId(String localTransactionId);

    List<Transaction> findByStatusAndPspNotified(TransactionStatus status, Boolean pspNotified);
}
