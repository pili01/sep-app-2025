package com.example.Bank.dto.account;

import lombok.Data;

@Data
public class AccountResponse {
    private Long id;
    private String accountHolderName;
    private String merchantId;
    private String accountNumber;
    private double balance;
    private String currency;
    private Long userId;
}