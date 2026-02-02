package com.example.PaymentServiceProviderSEP.dto;

import com.example.PaymentServiceProviderSEP.model.TransactionStatus;
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
    private TransactionStatus transactionStatus;
}
