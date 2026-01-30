package com.example.PaymentServiceProviderSEP.service;

import com.example.PaymentServiceProviderSEP.model.Transaction;
import com.example.PaymentServiceProviderSEP.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantWebhookSchedulerService {

    private final TransactionRepository transactionRepository;
    private final MerchantWebhookService merchantWebhookService;

    @Scheduled(fixedRate = 1000 * 60 * 5) // 5 minutes
    public void sendPendingWebhooks() {
        log.info("Starting scheduled merchant webhook notification check");

        try {
            List<Transaction> unnotifiedTransactions = transactionRepository.findUnnotifiedTransactions();

            if (unnotifiedTransactions.isEmpty()) {
                log.debug("No unnotified transactions found");
                return;
            }

            log.info("Found {} unnotified transactions", unnotifiedTransactions.size());

            for (Transaction transaction : unnotifiedTransactions) {
                try {
                    merchantWebhookService.notifyMerchant(transaction);
                } catch (Exception e) {
                    log.error("Failed to send webhook for transaction {}: {}",
                            transaction.getTransactionId(), e.getMessage());
                }
            }

            log.info("Completed merchant webhook notification check");
        } catch (Exception e) {
            log.error("Error during merchant webhook notification check: {}", e.getMessage(), e);
        }
    }
}
