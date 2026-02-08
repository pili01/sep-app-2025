package com.example.PaymentServiceProviderSEP.service;

import com.example.PaymentServiceProviderSEP.model.Transaction;
import com.example.PaymentServiceProviderSEP.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionService {
    private final TransactionRepository transactionRepository;

    @Transactional
    public Transaction createTransaction(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    public Transaction getTransactionById(long transactionId) {
        return  transactionRepository.findById(transactionId).orElseThrow(() -> new RuntimeException("Invalid transaction id " + transactionId));
    }
}
