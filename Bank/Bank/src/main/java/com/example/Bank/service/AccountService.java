package com.example.Bank.service;

import com.example.Bank.dto.account.AccountResponse;
import com.example.Bank.dto.account.CreateAccountRequest;
import com.example.Bank.model.Account;
import com.example.Bank.model.User;
import com.example.Bank.repository.AccountRepository;
import com.example.Bank.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

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

    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAllByDeletedFalse()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public AccountResponse createAccount(CreateAccountRequest request) {

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        accountRepository.findByUserId(user.getId())
                .ifPresent(a -> {
                    throw new RuntimeException("User already has an account");
                });

        Account account = new Account();
        account.setAccountHolderName(user.getName() + " " + user.getSurname());
        account.setMerchantId(request.getMerchantId());
        account.setAccountNumber(request.getAccountNumber());
        account.setBalance(request.getBalance());
        account.setCurrency(
                request.getCurrency() != null ? request.getCurrency() : "EUR"
        );
        account.setUser(user);
        account.setDeleted(false);

        Account saved = accountRepository.save(account);
        return mapToResponse(saved);
    }

    public void deleteAccount(Long id) {
        Account account = accountRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        account.setDeleted(true);
        accountRepository.save(account);
    }

    private AccountResponse mapToResponse(Account account) {
        AccountResponse response = modelMapper.map(account, AccountResponse.class);
        response.setUserId(account.getUser().getId());
        return response;
    }
}
