package com.example.Bank.dto.card;

import lombok.Data;

@Data
public class CardResponse {
    private Long id;
    private String cardNumber;
    private String cardholderName;
    private String expirationDate;
}