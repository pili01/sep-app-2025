package com.example.service;

import com.example.dto.CheckStatusRequest;
import com.example.dto.CheckStatusResponse;
import com.example.dto.PayPalOrderDetailsResponse;
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
    private final PayPalClient payPalClient;

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
                    .build();
        }

        try {
            // Pozovi PayPal API da proveri stvarni status
            PayPalOrderDetailsResponse orderDetails = payPalClient.getOrderDetails(
                    transaction.getAccessToken(),
                    transaction.getGlobalTransactionId());

            // Proveri amount i currency
            boolean amountMatches = false;
            boolean currencyMatches = false;
            boolean statusCompleted = false;

            if (orderDetails.getPurchaseUnits() != null && !orderDetails.getPurchaseUnits().isEmpty()) {
                PayPalOrderDetailsResponse.PurchaseUnit unit = orderDetails.getPurchaseUnits().get(0);

                // Proveri amount
                BigDecimal paypalAmount = new BigDecimal(unit.getAmount().getValue());
                amountMatches = transaction.getAmount().compareTo(paypalAmount) == 0 &&
                        request.getAmount().compareTo(paypalAmount) == 0;

                // Proveri currency
                currencyMatches = transaction.getCurrency().equals(unit.getAmount().getCurrencyCode()) &&
                        request.getCurrency().equals(unit.getAmount().getCurrencyCode());

                // Proveri status iz captures
                if (unit.getPayments() != null && unit.getPayments().getCaptures() != null
                        && !unit.getPayments().getCaptures().isEmpty()) {
                    PayPalOrderDetailsResponse.Capture capture = unit.getPayments().getCaptures().get(0);
                    statusCompleted = "COMPLETED".equals(capture.getStatus());
                }
            }

            // Ako se sve slaže, vrati SUCCESS
            if (amountMatches && currencyMatches && statusCompleted) {
                log.info("Payment verification successful for transaction: {}", request.getTransactionId());

                return CheckStatusResponse.builder()
                        .transactionId(request.getTransactionId())
                        .status("COMPLETED")
                        .verified(true)
                        .amount(transaction.getAmount())
                        .currency(transaction.getCurrency())
                        .message("Payment verified successfully")
                        .build();
            } else {
                // Ako se nešto ne slaže, označi kao FAILED
                log.error(
                        "Payment verification failed for transaction: {}. Amount match: {}, Currency match: {}, Status: {}",
                        request.getTransactionId(), amountMatches, currencyMatches, statusCompleted);

                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setErrorMessage(String.format(
                        "Verification failed - Amount match: %s, Currency match: %s, Status completed: %s",
                        amountMatches, currencyMatches, statusCompleted));
                transactionRepository.save(transaction);

                return CheckStatusResponse.builder()
                        .transactionId(request.getTransactionId())
                        .status("FAILED")
                        .verified(false)
                        .amount(transaction.getAmount())
                        .currency(transaction.getCurrency())
                        .message("Payment verification failed - data mismatch")
                        .build();
            }

        } catch (Exception e) {
            log.error("Error verifying payment with PayPal for transaction: {}", request.getTransactionId(), e);

            transaction.setStatus(TransactionStatus.ERROR);
            transaction.setErrorMessage("PayPal verification error: " + e.getMessage());
            transactionRepository.save(transaction);

            return CheckStatusResponse.builder()
                    .transactionId(request.getTransactionId())
                    .status("ERROR")
                    .verified(false)
                    .amount(transaction.getAmount())
                    .currency(transaction.getCurrency())
                    .message("Error verifying with PayPal: " + e.getMessage())
                    .build();
        }
    }
}
