package com.example.service;

import com.example.dto.CheckStatusRequest;
import com.example.dto.CheckStatusResponse;
import com.example.dto.CryptoConfig;
import com.example.dto.PaymentStatusCheckResult;
import com.example.model.Transaction;
import com.example.model.TransactionStatus;
import com.example.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentVerificationService {

    private final TransactionRepository transactionRepository;
    private final BitcoinClient bitcoinClient;

    @Transactional
    public CheckStatusResponse verifyPaymentStatus(CheckStatusRequest request) {
        log.info("Verifying payment status for transaction: {}", request.getTransactionId());

        // Pronađi transakciju u bazi
        Transaction transaction = transactionRepository.findByPspTransactionId(request.getTransactionId())
                .orElseThrow(
                        () -> new IllegalArgumentException("Transaction not found: " + request.getTransactionId()));

        // Označi da je PSP notifikovan
        transaction.setPspNotified(true);
        transactionRepository.save(transaction);

        // Proveri da li je transakcija već completed
        if (transaction.getStatus() != TransactionStatus.COMPLETED) {
            log.warn("Transaction {} is not in COMPLETED status, current status: {}",
                    request.getTransactionId(), transaction.getStatus());

            return CheckStatusResponse.builder()
                    .transactionId(request.getTransactionId())
                    .status(transaction.getStatus().name())
                    .verified(false)
                    .amount(transaction.getAmount())
                    .currency(transaction.getCurrency())
                    .message("Transaction is not completed")
                    .paymentMethodName("Crypto")
                    .build();
        }

        try {
            // Parsiraj crypto config
            CryptoConfig cryptoConfig = bitcoinClient.parseConfig(transaction.getMerchantConfig());

            // Pozovi blockchain API da proveri stvarni status
            PaymentStatusCheckResult blockchainCheck = bitcoinClient.checkPaymentStatus(
                    transaction.getBitcoinAddress(),
                    transaction.getBitcoinAmount(),
                    transaction.getRequiredConfirmations() != null ? transaction.getRequiredConfirmations() : 1,
                    transaction.getGlobalTransactionId(), // Koristi postojeći hash ako postoji
                    transaction.getCreatedAt()
            );

            // Proveri da li je payment pronađen na blockchain-u
            if (!blockchainCheck.isPaymentFound()) {
                log.warn("Payment not found on blockchain for transaction: {}", request.getTransactionId());

                return CheckStatusResponse.builder()
                        .transactionId(request.getTransactionId())
                        .status("PENDING")
                        .verified(false)
                        .amount(transaction.getAmount())
                        .currency(transaction.getCurrency())
                        .message("Payment not found on blockchain")
                        .paymentMethodName("Crypto")
                        .build();
            }

            // Proveri amount i currency
            boolean amountMatches = false;
            boolean currencyMatches = false;
            boolean statusConfirmed = false;

            // Proveri amount - poredi sa blockchain iznosom i request iznosom
            if (request.getAmount() != null && blockchainCheck.getReceivedAmount() != null) {
                // Proveri da li se blockchain iznos slaže sa našim iznosom
                BigDecimal difference = blockchainCheck.getReceivedAmount().subtract(transaction.getBitcoinAmount()).abs();
                boolean blockchainAmountMatches = difference.compareTo(new BigDecimal("0.00000001")) <= 0;

                // Proveri da li se request amount slaže sa našim fiat amount
                // (request.getAmount() je u fiat valuti, treba konvertovati u BTC)
                // Za sada proveravamo samo da li se blockchain iznos slaže
                amountMatches = blockchainAmountMatches;
            } else {
                // Ako nema request amount, proveri samo blockchain iznos
                if (blockchainCheck.getReceivedAmount() != null) {
                    BigDecimal difference = blockchainCheck.getReceivedAmount().subtract(transaction.getBitcoinAmount()).abs();
                    amountMatches = difference.compareTo(new BigDecimal("0.00000001")) <= 0;
                }
            }

            // Proveri currency - currency se čuva u fiat valuti, blockchain je uvek BTC
            // Za crypto, currency provera nije relevantna jer blockchain uvek vraća BTC
            currencyMatches = true; // Currency je uvek BTC na blockchain-u

            // Proveri da li je payment confirmed (ima dovoljno potvrda)
            statusConfirmed = blockchainCheck.isConfirmed() &&
                    blockchainCheck.getConfirmations() != null &&
                    blockchainCheck.getConfirmations() >= transaction.getRequiredConfirmations();

            // Ako se sve slaže, vrati SUCCESS
            if (amountMatches && currencyMatches && statusConfirmed) {
                log.info("Payment verification successful for transaction: {}", request.getTransactionId());

                return CheckStatusResponse.builder()
                        .transactionId(request.getTransactionId())
                        .status("COMPLETED")
                        .verified(true)
                        .amount(transaction.getAmount())
                        .currency(transaction.getCurrency())
                        .message("Payment verified successfully on blockchain")
                        .paymentMethodName("Crypto")
                        .build();
            } else {
                // Ako se nešto ne slaže, označi kao FAILED
                log.error(
                        "Payment verification failed for transaction: {}. Amount match: {}, Currency match: {}, Status confirmed: {}",
                        request.getTransactionId(), amountMatches, currencyMatches, statusConfirmed);

                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setErrorMessage(String.format(
                        "Verification failed - Amount match: %s, Currency match: %s, Status confirmed: %s",
                        amountMatches, currencyMatches, statusConfirmed));
                transactionRepository.save(transaction);

                return CheckStatusResponse.builder()
                        .transactionId(request.getTransactionId())
                        .status("FAILED")
                        .verified(false)
                        .amount(transaction.getAmount())
                        .currency(transaction.getCurrency())
                        .message("Payment verification failed - data mismatch")
                        .paymentMethodName("Crypto")
                        .build();
            }

        } catch (Exception e) {
            log.error("Error verifying payment with blockchain for transaction: {}", request.getTransactionId(), e);

            transaction.setStatus(TransactionStatus.ERROR);
            transaction.setErrorMessage("Blockchain verification error: " + e.getMessage());
            transactionRepository.save(transaction);

            return CheckStatusResponse.builder()
                    .transactionId(request.getTransactionId())
                    .status("ERROR")
                    .verified(false)
                    .amount(transaction.getAmount())
                    .currency(transaction.getCurrency())
                    .message("Error verifying with blockchain: " + e.getMessage())
                    .paymentMethodName("Crypto")
                    .build();
        }
    }
}
