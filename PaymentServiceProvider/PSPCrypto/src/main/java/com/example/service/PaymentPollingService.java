package com.example.service;

import com.example.dto.PaymentStatusCheckResult;
import com.example.model.Transaction;
import com.example.model.TransactionStatus;
import com.example.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentPollingService {

    private final TransactionRepository transactionRepository;
    private final BitcoinClient bitcoinClient;
    private final WebhookService webhookService;


     //Svako 2 minuta provjera PENDING i PROCESSING transakcija da li je payment stigao na blockchain

    @Scheduled(fixedRate = 1000 * 60 * 2)
    public void checkPendingPayments() {

        try {
            List<Transaction> pendingTransactions = transactionRepository
                    .findByStatus(TransactionStatus.PENDING);

            processTransactions(pendingTransactions);


            List<Transaction> processingTransactions = transactionRepository
                    .findByStatus(TransactionStatus.PROCESSING);

            if (!processingTransactions.isEmpty()) {
                processTransactions(processingTransactions);
            }

        } catch (Exception e) {
            log.error("Error during scheduled payment status check", e);
        }
    }


    @Transactional
    public void processTransactions(List<Transaction> transactions) {
        for (Transaction transaction : transactions) {
            try {

                // Provjeri status na blockchain-u
                String addressToCheck = transaction.getBitcoinAddress();

                PaymentStatusCheckResult checkResult = bitcoinClient.checkPaymentStatus(
                        addressToCheck,
                        transaction.getBitcoinAmount(),
                        transaction.getRequiredConfirmations() != null ? transaction.getRequiredConfirmations() : 1,
                        transaction.getGlobalTransactionId(),
                        transaction.getCreatedAt()
                );

                // Ako payment nije pronađen, nastavi (može biti da još nije potvrđen)
                if (!checkResult.isPaymentFound()) {
                    continue;
                }

                // Proveri da li je amount mismatch (premala količina)
                if (checkResult.getReceivedAmount() != null && transaction.getBitcoinAmount() != null) {
                    BigDecimal receivedAmount = checkResult.getReceivedAmount();
                    BigDecimal expectedAmount = transaction.getBitcoinAmount();
                    

                    // Ako je primljena količina manja od očekivane (sa tolerancijom)
                    BigDecimal difference = expectedAmount.subtract(receivedAmount);
                    BigDecimal tolerance = new BigDecimal("0.00000001");
                    
                    if (difference.compareTo(tolerance) > 0) {
                        // Premala količina - postavi FAILED
                        markTransactionAsFailed(transaction,
                                String.format("Insufficient amount: expected %s BTC, received %s BTC",
                                        expectedAmount, receivedAmount));
                        webhookService.notifyPaymentFailed(transaction);
                        continue;
                    }
                    // Ako je prevelika količina, prihvatamo je (korisnik je poslao više)
                }

                updateTransactionFromBlockchain(transaction, checkResult);


                if (checkResult.isConfirmed()) {
                    completeTransaction(transaction);
                } else {
                    if (transaction.getStatus() != TransactionStatus.PROCESSING) {
                        transaction.setStatus(TransactionStatus.PROCESSING);
                        transactionRepository.save(transaction);
                    }
                }

            } catch (Exception e) {
                // Postavi ERROR status za tehničke greške
                markTransactionAsError(transaction, "Error checking blockchain: " + e.getMessage());
                try {
                    webhookService.notifyPaymentError(transaction);
                } catch (Exception webhookException) {
                    log.error("Failed to send error webhook for transaction: {}", 
                            transaction.getPspTransactionId(), webhookException);
                }
            }
        }
    }


    private void updateTransactionFromBlockchain(Transaction transaction, PaymentStatusCheckResult checkResult) {
        if (transaction.getGlobalTransactionId() == null && checkResult.getTransactionHash() != null) {
            transaction.setGlobalTransactionId(checkResult.getTransactionHash());
        }

        if (checkResult.getConfirmations() != null) {
            transaction.setConfirmations(checkResult.getConfirmations());
        }

        if (transaction.getPaymentReceivedAt() == null && checkResult.getReceivedAt() != null) {
            transaction.setPaymentReceivedAt(checkResult.getReceivedAt());
        }

        transactionRepository.save(transaction);
    }


    private void completeTransaction(Transaction transaction) {
        if (transaction.getStatus() == TransactionStatus.COMPLETED) {
            return;
        }

        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        // webhook notifikacija PSP-u
        try {
            webhookService.notifyPaymentCompleted(transaction);
        } catch (Exception e) {
            log.error("Failed to send webhook notification for transaction: {}", 
                    transaction.getPspTransactionId(), e);
        }
    }

    private void markTransactionAsFailed(Transaction transaction, String errorMessage) {
        if (transaction.getStatus() == TransactionStatus.FAILED) {
            return;
        }

        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setErrorMessage(errorMessage);
        transaction.setCompletedAt(LocalDateTime.now());
        transactionRepository.save(transaction);
    }

    private void markTransactionAsError(Transaction transaction, String errorMessage) {
        if (transaction.getStatus() == TransactionStatus.ERROR) {
            return;
        }

        transaction.setStatus(TransactionStatus.ERROR);
        transaction.setErrorMessage(errorMessage);
        transaction.setCompletedAt(LocalDateTime.now());
        transactionRepository.save(transaction);
    }
}
