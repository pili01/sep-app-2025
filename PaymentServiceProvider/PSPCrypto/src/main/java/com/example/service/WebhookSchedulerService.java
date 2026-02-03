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

    /**
     * Svakih 5 minuta šalje webhook za sve transakcije koje su COMPLETED
     * ali još nisu notifikovane od strane PSP
     */
    @Scheduled(fixedRate = 1000 * 60 * 5) // 5 minuta = 300000 ms
    public void sendPendingWebhooks() {
        log.info("Starting scheduled webhook notification check");

        try {
            // Pronađi sve transakcije koje su COMPLETED ali nemaju pspNotified = true
            List<Transaction> completedTransactions = transactionRepository
                    .findByStatusAndPspNotified(TransactionStatus.COMPLETED, false);

            if (completedTransactions.isEmpty()) {
                log.debug("No completed transactions to notify");
                return;
            }

            log.info("Found {} completed transactions to notify", completedTransactions.size());

            for (Transaction transaction : completedTransactions) {
                try {
                    log.info("Sending webhook notification for transaction: {}", transaction.getPspTransactionId());
                    webhookService.notifyPaymentCompleted(transaction);
                } catch (Exception e) {
                    log.error("Failed to send webhook for transaction: {}", transaction.getPspTransactionId(), e);
                    // Nastavi sa sledećom transakcijom
                }
            }

            log.info("Completed scheduled webhook notification check");

            // Pronađi sve transakcije koje su FAILED ali nemaju pspNotified = true
            List<Transaction> failedTransactions = transactionRepository
                    .findByStatusAndPspNotified(TransactionStatus.FAILED, false);

            if (failedTransactions.isEmpty()) {
                log.debug("No failed transactions to notify");
                return;
            }

            log.info("Found {} failed transactions to notify", failedTransactions.size());

            for (Transaction transaction : failedTransactions) {
                try {
                    log.info("Sending webhook notification for transaction: {}", transaction.getPspTransactionId());
                    webhookService.notifyPaymentFailed(transaction);
                } catch (Exception e) {
                    log.error("Failed to send webhook for transaction: {}", transaction.getPspTransactionId(), e);
                }
            }

            // Pronađi sve transakcije koje su ERROR ali nemaju pspNotified = true
            List<Transaction> errorTransactions = transactionRepository
                    .findByStatusAndPspNotified(TransactionStatus.ERROR, false);

            if (errorTransactions.isEmpty()) {
                log.debug("No error transactions to notify");
                return;
            }

            log.info("Found {} error transactions to notify", errorTransactions.size());

            for (Transaction transaction : errorTransactions) {
                try {
                    log.info("Sending webhook notification for transaction: {}", transaction.getPspTransactionId());
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
