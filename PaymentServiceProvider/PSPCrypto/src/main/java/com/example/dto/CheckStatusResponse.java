package com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckStatusResponse {
    private String transactionId;
    private String status; // COMPLETED, FAILED, ERROR, PENDING
    private Boolean verified; // true if amount, currency match
    private BigDecimal amount;
    private String currency;
    private String message;
    private String paymentMethodName; // Added for WebShop verification (e.g., "Crypto", "Bitcoin")
}
