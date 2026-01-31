package com.example.Bank.controller;

import com.example.Bank.dto.account.AccountResponse;
import com.example.Bank.dto.account.CreateAccountRequest;
import com.example.Bank.service.AccountService;
import lombok.RequiredArgsConstructor;
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
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    // CREATE
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createAccount(
            @RequestBody CreateAccountRequest request
    ) {
        try {
            return ResponseEntity.ok(accountService.createAccount(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // DELETE (soft)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteAccount(@PathVariable Long id) {
        try {
            accountService.deleteAccount(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
