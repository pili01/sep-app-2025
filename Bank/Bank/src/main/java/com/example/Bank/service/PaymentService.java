package com.example.Bank.service;

import com.example.Bank.controller.PaymentController;
import com.example.Bank.dto.payment.PaymentDetailsResponse;
import com.example.Bank.dto.payment.PaymentProcessRequest;
import com.example.Bank.dto.payment.PaymentProcessResponse;
import com.example.Bank.dto.QrCodeData;
import com.example.Bank.model.Account;
import com.example.Bank.model.Card;
import com.example.Bank.model.PaymentTransaction;
import com.example.Bank.repository.AccountRepository;
import com.example.Bank.repository.CardRepository;
import com.example.Bank.repository.PaymentTransactionRepository;
import com.example.Bank.util.AuditLogger;
import com.example.Bank.util.CryptoService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final CardService cardService;
    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final PSPClientService pspClientService;
    private final QrCodeService qrCodeService;
    private final CryptoService cryptoService;

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private final AuditLogger audit;

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

    public byte[] generateQrCodeForPaymentTransaction(String paymentId) throws Exception {
        PaymentTransaction transaction = paymentTransactionRepository.findByPaymentId(paymentId).get();

        QrCodeData qrCodeData = new QrCodeData();

        Account merchantAccount = accountService.getAccountByMerchantId(transaction.getMerchantId());
        String accNumber = cryptoService.decrypt(merchantAccount.getAccountNumberEnc());

        qrCodeData.setR(accNumber);
        qrCodeData.setN(merchantAccount.getAccountHolderName());
        qrCodeData.setI(transaction.getAmount()); //zaokruzujem na 2 decimale
        qrCodeData.setS("Payment for rental in web shop");
        qrCodeData.setRO("00" + transaction.getId());

        return qrCodeService.generateQrCodeForTransaction(qrCodeData, 420);
    }

    //isto test akaunt i kard
    public String createTestAccountAndCard() {
        Account account = new Account();
        account.setMerchantId("TEST_MERCHANT");

        String accNumber = "1234567890";
        String accNumberHash = cryptoService.hashDeterministic(accNumber);
        String accNumberEnc = cryptoService.encrypt(accNumber);

        account.setAccountNumberEnc(accNumberEnc);
        account.setAccountNumberHash(accNumberHash);

        account.setBalance(1000.0);
        account.setCurrency("EUR");
        account.setDeleted(false);
        account = accountRepository.save(account);

        String pan = "4111111111111111";
        String panEnc = cryptoService.encrypt(pan);
        String cvvEnc = cryptoService.encrypt("123");

        Card card = new Card();
        card.setPanHash(cryptoService.hashDeterministic(pan));
        card.setPanEnc(cryptoService.encrypt(panEnc));
        card.setCardholderName("Test User");
        card.setExpirationDate("12/25");
        card.setCvvEnc(cvvEnc);
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
        log.info("Processing payment: paymentId={}, panHash={}", paymentId, cryptoService.hashDeterministic(request.getPan().replaceAll("\\D", "")));

        PaymentTransaction transaction = paymentTransactionRepository
                .findByPaymentId(paymentId)
                .orElseThrow(() -> {
                    String msg = "Payment not found: " + paymentId;
                    audit.info(msg);
                    log.warn(msg);
                    return new RuntimeException(msg);
                });

        if (LocalDateTime.now().isAfter(transaction.getExpiresAt())) {
            String redirectUrl = getErrorRedirectUrl(transaction);
            String msg = "Payment session expired: transactionId=" + transaction.getId();
            audit.info(msg);
            log.warn(msg);
            return new PaymentProcessResponse(false, "Payment session has expired", null, null, redirectUrl);
        }

        if (transaction.getUsed()) {
            String redirectUrl = getErrorRedirectUrl(transaction);
            String msg = "Payment already processed: transactionId=" + transaction.getId();
            audit.info(msg);
            log.warn(msg);
            return new PaymentProcessResponse(false, "Payment has already been processed", null, null, redirectUrl);
        }

        String panDigits = request.getPan().replaceAll("\\D", "");
        if (!cardService.validateLuhn(panDigits)) {
            String redirectUrl = getErrorRedirectUrl(transaction);
            String msg = "Invalid card number attempt: transactionId=" + transaction.getId();
            audit.info(msg);
            log.warn(msg);
            return new PaymentProcessResponse(false, "Invalid card number", null, null, redirectUrl);
        }

        if (!cardService.validateExpirationDate(request.getExpirationDate())) {
            String redirectUrl = getErrorRedirectUrl(transaction);
            String msg = "Invalid or expired card date: transactionId=" + transaction.getId();
            audit.info(msg);
            log.warn(msg);
            return new PaymentProcessResponse(false, "Invalid or expired card expiration date", null, null, redirectUrl);
        }

        String panHash = cryptoService.hashDeterministic(panDigits);
        Optional<Card> cardOpt = cardRepository.findByPanHashAndDeletedFalse(panHash);

        if (cardOpt.isEmpty()) {
            String redirectUrl = getErrorRedirectUrl(transaction);
            String msg = "Card not found or invalid details: panHash=" + panHash;
            audit.info(msg);
            log.warn(msg);
            return new PaymentProcessResponse(false, "Card not found or invalid card details", null, null, redirectUrl);
        }

        Card card = cardOpt.get();

        String decryptedCvv = cryptoService.decrypt(card.getCvvEnc());

        if (!card.getExpirationDate().equals(request.getExpirationDate()) ||
                !card.getCardholderName().equalsIgnoreCase(request.getCardHolderName()) ||
                !decryptedCvv.equals(request.getSecurityCode())) {
            String redirectUrl = getErrorRedirectUrl(transaction);
            String msg = "Card validation failed for transactionId=" + transaction.getId() + "panHash=" + card.getPanHash();
            audit.info(msg);
            log.warn(msg);
            return new PaymentProcessResponse(false, "Card not found or invalid card details", null, null, redirectUrl);
        }

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

            String msg = "Payment failed due to insufficient funds: transactionId=" + transaction.getId() + ", accountId=" + account.getId();
            audit.info(msg);
            log.info(msg);

            return new PaymentProcessResponse(false, "Insufficient funds", globalTransactionId, acquirerTimestamp.toString(), redirectUrl);
        }

        // SUCCESS slučaj - dovoljno sredstava
        // Oduzimam sredstva sa kartice korisnika
        double oldBalance = account.getBalance();
        account.setBalance(oldBalance - transaction.getAmount());
        accountRepository.save(account);

        String debitMsg = String.format(
                "Debit: transactionId=%s, fromAccountHash=%s, amount=%s, oldBalance=%s, newBalance=%s",
                transaction.getId(),
                account.getAccountNumberHash(),
                transaction.getAmount(),
                oldBalance,
                account.getBalance()
        );
        audit.info(debitMsg);
        log.info(debitMsg);

        // Prebacujem sredstva na merchant account
        Optional<Account> merchantAccountOpt = accountRepository.findByMerchantId(transaction.getMerchantId());
        if (merchantAccountOpt.isPresent()) {
            Account merchantAccount = merchantAccountOpt.get();
            double merchantOldBalance = merchantAccount.getBalance();
            merchantAccount.setBalance(merchantOldBalance + transaction.getAmount());
            accountRepository.save(merchantAccount);

            String creditMsg = String.format(
                    "Credit: transactionId=%s, toAccountHash=%s, amount=%s, oldBalance=%s, newBalance=%s",
                    transaction.getId(),
                    merchantAccount.getAccountNumberHash(),
                    transaction.getAmount(),
                    merchantOldBalance,
                    merchantAccount.getBalance()
            );
            audit.info(creditMsg);
            log.info(creditMsg);
        }

        paymentTransactionRepository.save(transaction);

        // slanje statusa Pspu i dobijanje redirectUrl-a
        String redirectUrl = pspClientService.sendPaymentStatus(
                transaction.getStan(),
                globalTransactionId,
                acquirerTimestamp,
                "SUCCESS"
        ).orElse(null);

        String msg = "Payment processed successfully: transactionId=" + transaction.getId() +
                ", globalTransactionId=" + globalTransactionId + ", amount=" + transaction.getAmount();
        audit.info(msg);
        log.info(msg);

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


    public PaymentProcessResponse processPaymentQR(QrCodeData qrCodeData,String email) {
        String ro = qrCodeData.getRO();
        String transactionId = ro.substring(2);

        PaymentTransaction transaction = paymentTransactionRepository
                .findById(Long.valueOf(transactionId))
                .orElseThrow(() -> new RuntimeException("TRANSID NOT FOUND"));


        Account fromAccount=accountService.getMyAccount(email);
        String accountNumber = qrCodeData.getR();
        String accNumberHash =  cryptoService.hashDeterministic(accountNumber);

        Account toAccount=accountRepository.findByAccountNumberHashAndDeletedFalse(accNumberHash)
                .orElseThrow(() -> new RuntimeException("Recipient account not found"));;

        String globalTransactionId = UUID.randomUUID().toString();
        LocalDateTime acquirerTimestamp = LocalDateTime.now();
        transaction.setGlobalTransactionId(globalTransactionId);
        transaction.setAcquirerTimestamp(acquirerTimestamp);

        if(fromAccount==null || toAccount==null)
            {throw new RuntimeException("Accounts not found");}

        log.info("Processing QR transaction: transactionId={}, fromAccountHash={}, toAccountHash={}, amount={}",
                transaction.getId(),
                fromAccount.getAccountNumberHash(),
                toAccount.getAccountNumberHash(),
                transaction.getAmount());

        if (fromAccount.getBalance() < transaction.getAmount()) {
            paymentTransactionRepository.save(transaction);

            String redirectUrl = pspClientService.sendPaymentStatus(
                    transaction.getStan(),
                    globalTransactionId,
                    acquirerTimestamp,
                    "FAILED"
            ).orElse(null);

            log.warn("Transaction FAILED due to insufficient funds: transactionId={}, fromAccountHash={}, amount={} {}",
                    transaction.getId(),
                    fromAccount.getAccountNumberHash(),
                    transaction.getAmount(),
                    transaction.getCurrency());

            return new PaymentProcessResponse(false, "Insufficient funds", globalTransactionId, acquirerTimestamp.toString(), redirectUrl);
        }

        // SUCCESS slučaj - dovoljno sredstava
        // Oduzimam sredstva sa kartice korisnika
        double fromOldBalance = fromAccount.getBalance();
        fromAccount.setBalance(fromOldBalance - transaction.getAmount());
        accountRepository.save(fromAccount);

        double toOldBalance = toAccount.getBalance();
        toAccount.setBalance(toOldBalance + transaction.getAmount());
        accountRepository.save(toAccount);

        // Audit logs
        String debitMsg = String.format(
                "Debit: transactionId=%s, fromAccountHash=%s, amount=%s, oldBalance=%s, newBalance=%s",
                transaction.getId(),
                fromAccount.getAccountNumberHash(),
                transaction.getAmount(),
                fromOldBalance,
                fromAccount.getBalance()
        );
        audit.info(debitMsg);
        log.info(debitMsg);

        String creditMsg = String.format(
                "Credit: transactionId=%s, toAccountHash=%s, amount=%s, oldBalance=%s, newBalance=%s",
                transaction.getId(),
                toAccount.getAccountNumberHash(),
                transaction.getAmount(),
                toOldBalance,
                toAccount.getBalance()
        );
        audit.info(creditMsg);
        log.info(creditMsg);


        paymentTransactionRepository.save(transaction);

        // slanje statusa Pspu i dobijanje redirectUrl-a
        String redirectUrl = pspClientService.sendPaymentStatus(
                transaction.getStan(),
                globalTransactionId,
                acquirerTimestamp,
                "SUCCESS"
        ).orElse(null);

        log.info("Transaction SUCCESS: transactionId={}, fromAccountHash={}, toAccountHash={}, amount={}",
                transaction.getId(),
                fromAccount.getAccountNumberHash(),
                toAccount.getAccountNumberHash(),
                transaction.getAmount());

        return new PaymentProcessResponse(true, "Payment processed successfully", globalTransactionId, acquirerTimestamp.toString(), redirectUrl);
    }
}

