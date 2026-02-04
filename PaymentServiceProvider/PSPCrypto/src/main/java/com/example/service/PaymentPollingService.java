package com.example.service;

import com.example.dto.CryptoConfig;
import com.example.dto.PaymentStatusCheckResult;
import com.example.model.Transaction;
import com.example.model.TransactionStatus;
import com.example.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servis koji periodično proverava blockchain za PENDING transakcije
 * i ažurira njihov status kada se payment primi
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentPollingService {

    private final TransactionRepository transactionRepository;
    private final BitcoinClient bitcoinClient;
    private final WebhookService webhookService;

    /**
     * Svakih 2 minuta proverava PENDING i PROCESSING transakcije
     * da li je payment stigao na blockchain
     * Smanjeno sa 30 sekundi na 2 minuta zbog rate limit-a
     */
    @Scheduled(fixedRate = 1000 * 60 * 2) // 2 minuta = 120000 ms (smanjeno zbog rate limit-a)
    public void checkPendingPayments() {
        log.debug("Starting scheduled payment status check");

        try {
            // Pronađi sve PENDING transakcije
            List<Transaction> pendingTransactions = transactionRepository
                    .findByStatus(TransactionStatus.PENDING);

            if (pendingTransactions.isEmpty()) {
                log.debug("No pending transactions to check");
            } else {
                log.info("Found {} pending transactions to check", pendingTransactions.size());
                processTransactions(pendingTransactions);
            }

            // Pronađi sve PROCESSING transakcije (payment pronađen ali još nije confirmed)
            List<Transaction> processingTransactions = transactionRepository
                    .findByStatus(TransactionStatus.PROCESSING);

            if (!processingTransactions.isEmpty()) {
                log.info("Found {} processing transactions to check", processingTransactions.size());
                processTransactions(processingTransactions);
            }

            log.debug("Completed scheduled payment status check");

        } catch (Exception e) {
            log.error("Error during scheduled payment status check", e);
        }
    }

    /**
     * Procesira listu transakcija i proverava njihov status na blockchain-u
     */
    @Transactional
    private void processTransactions(List<Transaction> transactions) {
        for (Transaction transaction : transactions) {
            try {
                log.debug("Checking payment status for transaction: {}", transaction.getPspTransactionId());

                // Parsiraj crypto config
                CryptoConfig cryptoConfig = bitcoinClient.parseConfig(transaction.getMerchantConfig());

                // Proveri status na blockchain-u
                // Prosleđujemo existingTransactionHash ako već postoji, i createdAt za proveru timestamp-a
                String addressToCheck = transaction.getBitcoinAddress();
                log.debug("Checking payment for transaction: {}, address from DB: '{}' (length: {})", 
                        transaction.getPspTransactionId(), 
                        addressToCheck, 
                        addressToCheck != null ? addressToCheck.length() : 0);
                
                PaymentStatusCheckResult checkResult = bitcoinClient.checkPaymentStatus(
                        addressToCheck,
                        transaction.getBitcoinAmount(),
                        transaction.getRequiredConfirmations() != null ? transaction.getRequiredConfirmations() : 1,
                        transaction.getGlobalTransactionId(), // Ako već imamo hash, proveri samo tu transakciju
                        transaction.getCreatedAt() // Timestamp za proveru da li je transakcija stigla posle kreiranja
                );

                if (!checkResult.isPaymentFound()) {
                    log.debug("Payment not found on blockchain for transaction: {}", transaction.getPspTransactionId());
                    continue; // Payment još nije stigao, proveri sledeći put
                }

                // Payment je pronađen!
                log.info("Payment found on blockchain for transaction: {}, Hash: {}, Confirmations: {}/{}",
                        transaction.getPspTransactionId(),
                        checkResult.getTransactionHash(),
                        checkResult.getConfirmations(),
                        transaction.getRequiredConfirmations());

                // Ažuriraj transakciju sa informacijama sa blockchain-a
                updateTransactionFromBlockchain(transaction, checkResult);

                // Proveri da li je payment confirmed (ima dovoljno potvrda)
                if (checkResult.isConfirmed()) {
                    // Payment je confirmed - označi kao COMPLETED
                    completeTransaction(transaction);
                } else {
                    // Payment je pronađen ali još nije confirmed - označi kao PROCESSING
                    if (transaction.getStatus() != TransactionStatus.PROCESSING) {
                        transaction.setStatus(TransactionStatus.PROCESSING);
                        transactionRepository.save(transaction);
                        log.info("Transaction {} moved to PROCESSING status (waiting for confirmations)",
                                transaction.getPspTransactionId());
                    }
                }

            } catch (Exception e) {
                log.error("Error checking payment status for transaction: {}", 
                        transaction.getPspTransactionId(), e);
                // Nastavi sa sledećom transakcijom
            }
        }
    }

    /**
     * Ažurira transakciju sa informacijama sa blockchain-a
     */
    private void updateTransactionFromBlockchain(Transaction transaction, PaymentStatusCheckResult checkResult) {
        // Postavi global transaction ID (hash) ako još nije postavljen
        if (transaction.getGlobalTransactionId() == null && checkResult.getTransactionHash() != null) {
            transaction.setGlobalTransactionId(checkResult.getTransactionHash());
        }

        // Ažuriraj broj potvrda
        if (checkResult.getConfirmations() != null) {
            transaction.setConfirmations(checkResult.getConfirmations());
        }

        // Postavi paymentReceivedAt ako još nije postavljen
        if (transaction.getPaymentReceivedAt() == null && checkResult.getReceivedAt() != null) {
            transaction.setPaymentReceivedAt(checkResult.getReceivedAt());
        }

        transactionRepository.save(transaction);
    }

    /**
     * Označi transakciju kao COMPLETED i pošalji webhook
     */
    private void completeTransaction(Transaction transaction) {
        if (transaction.getStatus() == TransactionStatus.COMPLETED) {
            log.debug("Transaction {} is already COMPLETED", transaction.getPspTransactionId());
            return; // Već je completed
        }

        log.info("Completing transaction: {}", transaction.getPspTransactionId());

        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        // Pošalji webhook notifikaciju PSP-u
        try {
            webhookService.notifyPaymentCompleted(transaction);
            log.info("Webhook notification sent for completed transaction: {}", transaction.getPspTransactionId());
        } catch (Exception e) {
            log.error("Failed to send webhook notification for transaction: {}", 
                    transaction.getPspTransactionId(), e);
            // Ne baci exception - webhook će biti poslat sledeći put kroz WebhookSchedulerService
        }
    }
}
