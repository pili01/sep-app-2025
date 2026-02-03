package com.example.Bank.controller;

import com.example.Bank.dto.account.AccountResponse;
import com.example.Bank.dto.account.CreateAccountRequest;
import com.example.Bank.service.AccountService;
import com.example.Bank.util.AuditLogger;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bank/accounts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AccountController {
    private final AccountService accountService;

    private static final Logger log = LoggerFactory.getLogger(AccountController.class);

    // uri za dobijanje merchant id kada se web shop pretplati na uslugu placanja bankom
    @PostMapping("/merchant-id")
    public ResponseEntity<?> getMerchantIdForWebShop(@RequestBody Map<String, String> accountNumber) {
        try {
            String merchantId = accountService.getMerchantIdFromAccountNumber(accountNumber.get("accountNumber"));
            return ResponseEntity.ok(Map.of("merchantId", merchantId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyAccount() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth != null ? auth.getName() : "unknown";
            return ResponseEntity.ok(accountService.getMyAccountData(email));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        log.info("Fetching all accounts (admin)");
        try {
            var accounts = accountService.getAllAccounts();
            log.info("Fetched {} accounts", accounts.size());
            return ResponseEntity.ok(accounts);
        } catch (Exception e) {
            log.error("Failed to fetch all accounts: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    // CREATE
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createAccount(
            @RequestBody CreateAccountRequest request
    ) {
        log.info("Creating account for accountNumber={}", request.getAccountNumber());
        try {
            var created = accountService.createAccount(request);
            log.info("Successfully created account id={}", created.getId());
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            log.error("Failed to create account for accountNumber={}: {}", request.getAccountNumber(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // DELETE (soft)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteAccount(@PathVariable Long id) {
        log.info("Deleting account id={}", id);
        try {
            accountService.deleteAccount(id);
            log.info("Successfully deleted account id={}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Failed to delete account id={}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
