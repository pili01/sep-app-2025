package com.example.Bank.service;

import com.example.Bank.model.Account;
import com.example.Bank.repository.AccountRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
