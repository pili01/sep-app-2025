package com.example.Bank.service;

import com.example.Bank.model.Account;
import com.example.Bank.repository.AccountRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;

    public String getMerchantIdFromAccountNumber(String accountNumber) {
        return accountRepository
                .findByAccountNumberAndDeletedFalse(accountNumber)
                .map(Account::getMerchantId)
                .orElseThrow(() -> new RuntimeException("Account not found for account number: " + accountNumber));
    }

    public Account getAccountByMerchantId(String merchantId) {
        return accountRepository
                .findByMerchantIdAndDeletedFalse(merchantId)
                .orElseThrow(() -> new RuntimeException("Account not found for merchantID: " + merchantId));
    }

    public Map<String, Object> getMyAccountData(String email) {
        Account account = accountRepository
                .findByUserEmailAndDeletedFalse(email)
                .orElseThrow(() -> new RuntimeException("Account not found for user email: " + email));
        return Map.of(
                "accountNumber", account.getAccountNumber(),
                "accountHolderName", account.getAccountHolderName()
        );
    }

    public Account getMyAccount(String email) {
        Account account = accountRepository
                .findByUserEmailAndDeletedFalse(email)
                .orElseThrow(() -> new RuntimeException("Account not found for user email: " + email));
        return account;
    }
}
