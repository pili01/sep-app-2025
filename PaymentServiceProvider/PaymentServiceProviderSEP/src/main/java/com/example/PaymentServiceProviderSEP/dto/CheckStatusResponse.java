package com.example.PaymentServiceProviderSEP.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckStatusResponse {
    private String transactionId;
    private String status;
    private Boolean verified;
    private String paymentMethodName;
    private String message;
}
