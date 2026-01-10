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
    private final PSPClientService pspClientService;
    
    public PaymentService(
            PaymentTransactionRepository paymentTransactionRepository,
            CardValidationService cardValidationService,
            CardRepository cardRepository,
            AccountRepository accountRepository,
            PSPClientService pspClientService) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.cardValidationService = cardValidationService;
        this.cardRepository = cardRepository;
        this.accountRepository = accountRepository;
        this.pspClientService = pspClientService;
    }

    //test transakcije
    public PaymentTransaction createTestTransaction() {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setPaymentId(UUID.randomUUID().toString());
        transaction.setAmount(100.0);
        transaction.setCurrency("EUR");
        transaction.setMerchantId("TEST_MERCHANT");
        transaction.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        transaction.setUsed(false);
        
        return paymentTransactionRepository.save(transaction);
    }
    
    public PaymentTransaction createPaymentTransaction(Double amount, String currency, String merchantId, String stan) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setPaymentId(UUID.randomUUID().toString());
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setMerchantId(merchantId);
        transaction.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        transaction.setUsed(false);
        transaction.setStan(stan); // Čuva STAN koji je poslao PSP
        
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
            transaction.getMerchantId(),
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
            String redirectUrl = getErrorRedirectUrl(transaction);
            return new PaymentProcessResponse(false, "Payment session has expired", null, null, redirectUrl);
        }

        if (transaction.getUsed()) {
            String redirectUrl = getErrorRedirectUrl(transaction);
            return new PaymentProcessResponse(false, "Payment has already been processed", null, null, redirectUrl);
        }

        String panDigits = request.getPan().replaceAll("\\D", "");
        if (!cardValidationService.validateLuhn(panDigits)) {
            String redirectUrl = getErrorRedirectUrl(transaction);
            return new PaymentProcessResponse(false, "Invalid card number", null, null, redirectUrl);
        }

        if (!cardValidationService.validateExpirationDate(request.getExpirationDate())) {
            String redirectUrl = getErrorRedirectUrl(transaction);
            return new PaymentProcessResponse(false, "Invalid or expired card expiration date", null, null, redirectUrl);
        }

        Optional<Card> cardOpt = cardRepository.findByCardNumberAndCvvAndCardholderNameAndExpirationDateAndDeletedFalse(
            panDigits,
            request.getSecurityCode(),
            request.getCardHolderName(),
            request.getExpirationDate()
        );
        
        if (cardOpt.isEmpty()) {
            String redirectUrl = getErrorRedirectUrl(transaction);
            return new PaymentProcessResponse(false, "Card not found or invalid card details", null, null, redirectUrl);
        }
        
        Card card = cardOpt.get();

        Account account = card.getAccount();

        String globalTransactionId = UUID.randomUUID().toString();
        LocalDateTime acquirerTimestamp = LocalDateTime.now();
        transaction.setGlobalTransactionId(globalTransactionId);
        transaction.setAcquirerTimestamp(acquirerTimestamp);
        transaction.setUsed(true);
        
        if (account.getBalance() < transaction.getAmount()) {
            paymentTransactionRepository.save(transaction);

            String redirectUrl = pspClientService.sendPaymentStatus(
                    transaction.getStan(),
                    globalTransactionId,
                    acquirerTimestamp,
                    "FAILED"
            ).orElse(null);
            
            return new PaymentProcessResponse(false, "Insufficient funds", globalTransactionId, acquirerTimestamp.toString(), redirectUrl);
        }

        // SUCCESS slučaj - dovoljno sredstava
        // Oduzimam sredstva sa kartice korisnika
        account.setBalance(account.getBalance() - transaction.getAmount());
        accountRepository.save(account);
        
        // Prebacujem sredstva na merchant account
        Optional<Account> merchantAccountOpt = accountRepository.findByMerchantId(transaction.getMerchantId());
        if (merchantAccountOpt.isPresent()) {
            Account merchantAccount = merchantAccountOpt.get();
            merchantAccount.setBalance(merchantAccount.getBalance() + transaction.getAmount());
            accountRepository.save(merchantAccount);
        }
        
        paymentTransactionRepository.save(transaction);
        
        // slanje statusa Pspu i dobijanje redirectUrl-a
        String redirectUrl = pspClientService.sendPaymentStatus(
                transaction.getStan(),
                globalTransactionId,
                acquirerTimestamp,
                "SUCCESS"
        ).orElse(null);
        
        return new PaymentProcessResponse(true, "Payment processed successfully", globalTransactionId, acquirerTimestamp.toString(), redirectUrl);
    }


    private String getErrorRedirectUrl(PaymentTransaction transaction) {
        if (transaction.getStan() != null) {
            String globalTransactionId = UUID.randomUUID().toString();
            LocalDateTime acquirerTimestamp = LocalDateTime.now();
            
            Optional<String> redirectUrlOpt = pspClientService.sendPaymentStatus(
                    transaction.getStan(),
                    globalTransactionId,
                    acquirerTimestamp,
                    "ERROR"
            );
            
            return redirectUrlOpt.orElse(null);
        }
        
        // Ako nema STAN-a, ne može da kontaktira PSP
        return null;
    }
}

