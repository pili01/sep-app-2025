package com.example.repository;

import com.example.model.Transaction;
import com.example.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByPspTransactionId(String pspTransactionId);

    Optional<Transaction> findByGlobalTransactionId(String globalTransactionId);

    List<Transaction> findByStatusAndPspNotified(TransactionStatus status, Boolean pspNotified);

    List<Transaction> findByStatus(TransactionStatus status);
}
