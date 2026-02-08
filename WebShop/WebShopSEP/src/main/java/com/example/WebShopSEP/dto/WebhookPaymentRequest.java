package com.example.WebShopSEP.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebhookPaymentRequest {
    private String transactionId;
    private String status;
    private Double amount;
    private String currency;
    private String pspTimestamp;
}
