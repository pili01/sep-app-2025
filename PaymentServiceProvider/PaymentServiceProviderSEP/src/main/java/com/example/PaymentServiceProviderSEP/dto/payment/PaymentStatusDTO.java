package com.example.PaymentServiceProviderSEP.dto.payment;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusDTO {
    @NotBlank(message = "STAN is required")
    private String stan;

    @NotBlank(message = "Global Transaction ID is required")
    private String globalTransactionId;

    @NotBlank(message = "Acquirer Timestamp is required")
    private String acquirerTimestamp; // ISO format string

    @NotBlank(message = "Status is required")
    private String status; // "SUCCESS" or "FAILED"
}





