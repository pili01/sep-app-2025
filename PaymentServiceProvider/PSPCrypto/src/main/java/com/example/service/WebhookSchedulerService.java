package com.example.service;

import com.example.model.Transaction;
import com.example.model.TransactionStatus;
import com.example.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookSchedulerService {

    private final TransactionRepository transactionRepository;
    private final WebhookService webhookService;

     //Svakih 5 minuta šalje webhook za sve transakcije koje su COMPLETED ali još nisu notifikovane od strane PSP

    @Scheduled(fixedRate = 1000 * 60 * 5)
    public void sendPendingWebhooks() {
        try {
            // Pronađi sve transakcije koje su COMPLETED ali nemaju pspNotified = true
            List<Transaction> completedTransactions = transactionRepository
                    .findByStatusAndPspNotified(TransactionStatus.COMPLETED, false);

            if (completedTransactions.isEmpty()) {
                return;
            }

            for (Transaction transaction : completedTransactions) {
                try {
                    webhookService.notifyPaymentCompleted(transaction);
                } catch (Exception e) {
                    log.error("Failed to send webhook for transaction: {}", transaction.getPspTransactionId(), e);

                }
            }


            List<Transaction> failedTransactions = transactionRepository
                    .findByStatusAndPspNotified(TransactionStatus.FAILED, false);

            if (failedTransactions.isEmpty()) {
                return;
            }


            for (Transaction transaction : failedTransactions) {
                try {
                    webhookService.notifyPaymentFailed(transaction);
                } catch (Exception e) {
                    log.error("Failed to send webhook for transaction: {}", transaction.getPspTransactionId(), e);
                }
            }

            // Pronađi sve transakcije koje su ERROR ali nemaju pspNotified = true
            List<Transaction> errorTransactions = transactionRepository
                    .findByStatusAndPspNotified(TransactionStatus.ERROR, false);

            if (errorTransactions.isEmpty()) {
                return;
            }


            for (Transaction transaction : errorTransactions) {
                try {
                    webhookService.notifyPaymentError(transaction);
                } catch (Exception e) {
                    log.error("Failed to send webhook for transaction: {}", transaction.getPspTransactionId(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error during scheduled webhook notification", e);
        }
    }
}
