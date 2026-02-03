package com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionStatusResponse {
    private String transactionId;
    private String status;
    private BigDecimal amount;
    private String currency;
    private BigDecimal bitcoinAmount;
    private String bitcoinAddress;
    private Integer confirmations;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String errorMessage;
}
