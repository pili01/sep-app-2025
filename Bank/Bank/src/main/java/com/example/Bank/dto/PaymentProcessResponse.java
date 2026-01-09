package com.example.Bank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentProcessResponse {
    private Boolean success;
    private String message;
    private String globalTransactionId; //ako sam dobro skonto gen se nakon uspjesnog placanja
    private String acquirerTimestamp; // ACQUIRER_TIMESTAMP - timestamp kada je banka obradila transakciju
    private String redirectUrl; // URL za redirekciju korisnika na WebShop status stranicu
}




