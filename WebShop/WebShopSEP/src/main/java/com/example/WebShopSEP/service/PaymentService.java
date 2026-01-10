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

import java.util.Map;
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
            Map<String, String> pspResponse = pspClientService.getTransactionStatusWithPaymentMethod(transactionId);
            String pspStatus = pspResponse.get("status");
            String paymentMethod = pspResponse.get("paymentMethod");

            boolean paymentMethodUpdated = false;
            // Ažuriranje paymentMethod-a ako je dostupan i još nije postavljen
            if (paymentMethod != null && !paymentMethod.isEmpty() && 
                (transaction.getPaymentMethod() == null || transaction.getPaymentMethod().isEmpty())) {
                transaction.setPaymentMethod(paymentMethod);
                paymentMethodUpdated = true;
            }

            boolean statusChanged = false;
            if ("COMPLETED".equalsIgnoreCase(pspStatus)) {
                if (transaction.getStatus() != TransactionStatus.COMPLETED) {
                    transaction.setStatus(TransactionStatus.COMPLETED);
                    // Ažuriranje statusa rentala ako je transakcija uspela
                    updateRentalStatus(transaction.getRentalId(), RentalStatus.PURCHASED);
                    statusChanged = true;
                }
            } else if ("FAILED".equalsIgnoreCase(pspStatus)) {
                if (transaction.getStatus() != TransactionStatus.FAILED) {
                    transaction.setStatus(TransactionStatus.FAILED);
                    statusChanged = true;
                }
            } else if ("ERROR".equalsIgnoreCase(pspStatus)) {
                if (transaction.getStatus() != TransactionStatus.ERROR) {
                    transaction.setStatus(TransactionStatus.ERROR);
                    statusChanged = true;
                }
            }

            // Sačuvaj transakciju ako je status promenjen ili paymentMethod ažuriran
            if (statusChanged || paymentMethodUpdated) {
                transactionRepository.save(transaction);
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

