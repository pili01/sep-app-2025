package com.example.PaymentServiceProviderSEP.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInitRequestDTO {

    @NotBlank(message = "Merchant ID is required")
    private String merchantId;

    @NotBlank(message = "Merchant password is required")
    private String merchantPassword;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotBlank(message = "Merchant order ID is required")
    private String merchantOrderId;

    private Timestamp merchantTimestamp;
}
