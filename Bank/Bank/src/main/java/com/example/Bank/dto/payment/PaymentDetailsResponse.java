package com.example.Bank.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentDetailsResponse {
    private Double amount;
    private String currency;
    private String merchantId;
    private LocalDateTime expiresAt;
    private List<String> acceptedCardTypes; // visa i mastercard
    private Boolean expired;
    private Boolean used;
}






