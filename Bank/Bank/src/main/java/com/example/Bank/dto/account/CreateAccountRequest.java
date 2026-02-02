package com.example.Bank.dto.account;

import lombok.Data;

@Data
public class CreateAccountRequest {
    private String merchantId;
    private String accountNumber;
    private double balance;
    private String currency;
    private Long userId;
}