package com.example.Bank.controller;

import com.example.Bank.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/bank/accounts")
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
}
