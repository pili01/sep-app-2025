package com.example.Bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {
    @NotBlank(message = "Merchant ID is required")
    private String merchantId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;
    
    @NotBlank(message = "Currency is required")
    private String currency;
    
    @NotBlank(message = "STAN is required")
    private String STAN;

    @NotBlank(message = "pspTimestamp is required")
    private String pspTimestamp;

    private String successUrl; // URL za redirekciju nakon uspešnog plaćanja

    private String failedUrl; // URL za redirekciju nakon neuspešnog plaćanja

    private String errorUrl; // URL za redirekciju u slučaju greške
}
