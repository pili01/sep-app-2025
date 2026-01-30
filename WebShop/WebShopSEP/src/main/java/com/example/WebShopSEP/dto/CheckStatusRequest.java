package com.example.WebShopSEP.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckStatusRequest {
    private String transactionId;
    private Double amount;
    private String currency;
}
