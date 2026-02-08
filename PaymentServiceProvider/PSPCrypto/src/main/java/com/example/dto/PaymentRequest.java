package com.example.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {


    @NotNull(message = "Transaction ID is required")
    private Long transactionId;


    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotBlank(message = "Merchant config is required")
    private String merchantConfig;

    @NotBlank(message = "Webhook URL is required")
    private String webhookUrl;

    @NotBlank(message = "Success URL is required")
    private String successUrl;

    @NotBlank(message = "Failed URL is required")
    private String failedUrl;

    @NotBlank(message = "Error URL is required")
    private String errorUrl;
}
