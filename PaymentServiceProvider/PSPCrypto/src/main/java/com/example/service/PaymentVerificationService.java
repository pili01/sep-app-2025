package com.example.service;

import com.example.dto.CheckStatusRequest;
import com.example.dto.CheckStatusResponse;
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
        Transaction transaction = findAndMarkTransaction(request.getTransactionId());

        // Proveri da li je transakcija COMPLETED
        if (transaction.getStatus() != TransactionStatus.COMPLETED) {
            return buildNotCompletedResponse(transaction, request);
        }

        try {
            PaymentStatusCheckResult blockchainCheck = verifyPaymentOnBlockchain(transaction);


            if (!blockchainCheck.isPaymentFound()) {
                return buildPaymentNotFoundResponse(transaction, request);
            }

            boolean amountMatches = verifyAmount(transaction, blockchainCheck, request);
            boolean currencyMatches = verifyCurrency();
            boolean statusConfirmed = verifyConfirmations(transaction, blockchainCheck);

            // Ako se sve slaže, vrati SUCCESS
            if (amountMatches && currencyMatches && statusConfirmed) {
                log.info("Payment verification successful for transaction: {}", request.getTransactionId());
                return buildSuccessResponse(transaction, request);
            } else {
                return buildFailedResponse(transaction, request, amountMatches, currencyMatches, statusConfirmed);
            }

        } catch (Exception e) {
            return buildErrorResponse(transaction, request, e);
        }
    }


     // Pronalazi transakciju i označava da je PSP notifikovan

    private Transaction findAndMarkTransaction(String transactionId) {
        Transaction transaction = transactionRepository.findByPspTransactionId(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        transaction.setPspNotified(true);
        transactionRepository.save(transaction);

        return transaction;
    }


    private PaymentStatusCheckResult verifyPaymentOnBlockchain(Transaction transaction) {
        return bitcoinClient.checkPaymentStatus(
                transaction.getBitcoinAddress(),
                transaction.getBitcoinAmount(),
                transaction.getRequiredConfirmations() != null ? transaction.getRequiredConfirmations() : 1,
                transaction.getGlobalTransactionId(),
                transaction.getCreatedAt()
        );
    }


    private boolean verifyAmount(Transaction transaction, PaymentStatusCheckResult blockchainCheck, CheckStatusRequest request) {
        if (request.getAmount() != null && blockchainCheck.getReceivedAmount() != null) {
            BigDecimal difference = blockchainCheck.getReceivedAmount().subtract(transaction.getBitcoinAmount()).abs();
            return difference.compareTo(new BigDecimal("0.00000001")) <= 0;
        } else {
            if (blockchainCheck.getReceivedAmount() != null) {
                BigDecimal difference = blockchainCheck.getReceivedAmount().subtract(transaction.getBitcoinAmount()).abs();
                return difference.compareTo(new BigDecimal("0.00000001")) <= 0;
            }
        }
        return false;
    }


    private boolean verifyCurrency() {
        return true;
    }


    private boolean verifyConfirmations(Transaction transaction, PaymentStatusCheckResult blockchainCheck) {
        return blockchainCheck.isConfirmed() &&
                blockchainCheck.getConfirmations() != null &&
                blockchainCheck.getConfirmations() >= transaction.getRequiredConfirmations();
    }


    private CheckStatusResponse buildNotCompletedResponse(Transaction transaction, CheckStatusRequest request) {
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


    private CheckStatusResponse buildPaymentNotFoundResponse(Transaction transaction, CheckStatusRequest request) {
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


    private CheckStatusResponse buildSuccessResponse(Transaction transaction, CheckStatusRequest request) {
        return CheckStatusResponse.builder()
                .transactionId(request.getTransactionId())
                .status("COMPLETED")
                .verified(true)
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .message("Payment verified successfully on blockchain")
                .paymentMethodName("Crypto")
                .build();
    }


    private CheckStatusResponse buildFailedResponse(Transaction transaction, CheckStatusRequest request,
                                                     boolean amountMatches, boolean currencyMatches, boolean statusConfirmed) {
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


    private CheckStatusResponse buildErrorResponse(Transaction transaction, CheckStatusRequest request, Exception e) {
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
