package com.example.Bank.service;

import com.example.Bank.dto.PaymentDetailsResponse;
import com.example.Bank.dto.PaymentProcessRequest;
import com.example.Bank.dto.PaymentProcessResponse;
import com.example.Bank.model.Account;
import com.example.Bank.model.Card;
import com.example.Bank.model.PaymentTransaction;
import com.example.Bank.repository.AccountRepository;
import com.example.Bank.repository.CardRepository;
import com.example.Bank.repository.PaymentTransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PaymentService {
    
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final CardValidationService cardValidationService;
    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;
    
    public PaymentService(
            PaymentTransactionRepository paymentTransactionRepository,
            CardValidationService cardValidationService,
            CardRepository cardRepository,
            AccountRepository accountRepository) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.cardValidationService = cardValidationService;
        this.cardRepository = cardRepository;
        this.accountRepository = accountRepository;
    }

    //test transakcije
    public PaymentTransaction createTestTransaction() {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setPaymentId(UUID.randomUUID().toString());
        transaction.setAmount(100.0);
        transaction.setCurrency("EUR");
        transaction.setMerchantName("Test Merchant");
        transaction.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        transaction.setUsed(false);
        
        return paymentTransactionRepository.save(transaction);
    }
    
    public PaymentTransaction createPaymentTransaction(Double amount, String currency, String merchantName) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setPaymentId(UUID.randomUUID().toString());
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setMerchantName(merchantName);
        transaction.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        transaction.setUsed(false);
        
        return paymentTransactionRepository.save(transaction);
    }
    
    //isto test akaunt i kard
    public String createTestAccountAndCard() {
        Account account = new Account();
        account.setMerchantId("TEST_MERCHANT");
        account.setAccountNumber("1234567890");
        account.setBalance(1000.0);
        account.setCurrency("EUR");
        account.setDeleted(false);
        account = accountRepository.save(account);

        Card card = new Card();
        card.setCardNumber("4111111111111111");
        card.setCardholderName("Test User");
        card.setExpirationDate("12/25");
        card.setCvv("123");
        card.setDeleted(false);
        card.setAccount(account);
        cardRepository.save(card);
        
        return "Test Account and Card created!\n" +
               "Card Number: 4111111111111111\n" +
               "Cardholder Name: Test User\n" +
               "Expiration Date: 12/25\n" +
               "CVV: 123\n" +
               "Account Balance: 1000.0 EUR";
    }

    public PaymentDetailsResponse getPaymentDetails(String paymentId) {
        PaymentTransaction transaction = paymentTransactionRepository
            .findByPaymentId(paymentId)
            .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        LocalDateTime now = LocalDateTime.now();
        boolean expired = now.isAfter(transaction.getExpiresAt());

        List<String> acceptedCardTypes = Arrays.asList("VISA", "MASTERCARD");
        
        return new PaymentDetailsResponse(
            transaction.getAmount(),
            transaction.getCurrency(),
            transaction.getMerchantName(),
            transaction.getExpiresAt(),
            acceptedCardTypes,
            expired,
            transaction.getUsed()
        );
    }
    

    public PaymentProcessResponse processPayment(String paymentId, PaymentProcessRequest request) {
        PaymentTransaction transaction = paymentTransactionRepository
            .findByPaymentId(paymentId)
            .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (LocalDateTime.now().isAfter(transaction.getExpiresAt())) {
            return new PaymentProcessResponse(false, "Payment session has expired", null);
        }

        if (transaction.getUsed()) {
            return new PaymentProcessResponse(false, "Payment has already been processed", null);
        }

        String panDigits = request.getPan().replaceAll("\\D", "");
        if (!cardValidationService.validateLuhn(panDigits)) {
            return new PaymentProcessResponse(false, "Invalid card number", null);
        }

        if (!cardValidationService.validateExpirationDate(request.getExpirationDate())) {
            return new PaymentProcessResponse(false, "Invalid or expired card expiration date", null);
        }

        Optional<Card> cardOpt = cardRepository.findByCardNumberAndCvvAndCardholderNameAndExpirationDateAndDeletedFalse(
            panDigits,
            request.getSecurityCode(),
            request.getCardHolderName(),
            request.getExpirationDate()
        );
        
        if (cardOpt.isEmpty()) {
            return new PaymentProcessResponse(false, "Card not found or invalid card details", null);
        }
        
        Card card = cardOpt.get();

        Account account = card.getAccount();
        if (account.getBalance() < transaction.getAmount()) {
            return new PaymentProcessResponse(false, "Insufficient funds", null);
        }

        //ovdje bi trebalo da se novac prebaci na racun prodavca
        account.setBalance(account.getBalance() - transaction.getAmount());
        accountRepository.save(account);

        transaction.setUsed(true);
        paymentTransactionRepository.save(transaction);
        
        // ovo vidit treba li ovje jesam obro skonto
        String globalTransactionId = UUID.randomUUID().toString();
        
        return new PaymentProcessResponse(true, "Payment processed successfully", globalTransactionId);
    }
}

