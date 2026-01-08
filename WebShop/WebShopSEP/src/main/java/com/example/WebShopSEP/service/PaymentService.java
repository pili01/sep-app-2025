package com.example.WebShopSEP.service;

import com.example.WebShopSEP.model.Rental;
import com.example.WebShopSEP.model.RentalStatus;
import com.example.WebShopSEP.model.Transaction;
import com.example.WebShopSEP.model.TransactionStatus;
import com.example.WebShopSEP.repository.RentalRepository;
import com.example.WebShopSEP.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final RentalRepository rentalRepository;
    private final PSPClientService pspClientService;

    public Transaction getTransactionStatus(String transactionId) {
        Optional<Transaction> transactionOpt = transactionRepository.findByTransactionId(transactionId);
        if (transactionOpt.isEmpty()) {
            throw new RuntimeException("Transaction not found: " + transactionId);
        }

        Transaction transaction = transactionOpt.get();

        // Proveravam status u PSP-u i ažuriram lokalnu transakciju
        try {
            String pspStatus = pspClientService.getTransactionStatus(transactionId);

            if ("COMPLETED".equalsIgnoreCase(pspStatus)) {
                if (transaction.getStatus() != TransactionStatus.COMPLETED) {
                    transaction.setStatus(TransactionStatus.COMPLETED);
                    // Ažuriranje statusa rentala ako je transakcija uspela
                    updateRentalStatus(transaction.getRentalId(), RentalStatus.PURCHASED);
                    transactionRepository.save(transaction);
                }
            } else if ("FAILED".equalsIgnoreCase(pspStatus)) {
                if (transaction.getStatus() != TransactionStatus.FAILED) {
                    transaction.setStatus(TransactionStatus.FAILED);
                    transactionRepository.save(transaction);
                }
            }
        } catch (Exception e) {
            // Ako ne mogu da dobijem status iz PSP-a, vraćam trenutni status
            System.err.println("Failed to sync status with PSP for transaction " + transactionId + ": " + e.getMessage());
        }

        return transaction;
    }

    private void updateRentalStatus(Long rentalId, RentalStatus status) {
        Optional<Rental> rentalOpt = rentalRepository.findById(rentalId);
        if (rentalOpt.isPresent()) {
            Rental rental = rentalOpt.get();
            if (rental.getStatus() == RentalStatus.DRAFT && status == RentalStatus.PURCHASED) {
                rental.setStatus(status);
                rentalRepository.save(rental);
            }
        }
    }
}

