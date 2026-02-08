package com.example.Bank.dto.card;

import lombok.Data;

@Data
public class CreateCardRequest {
    private Long accountId;
    private String cardNumber;
    private String expirationDate;
    private String cvv;
}