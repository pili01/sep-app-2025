package com.example.PaymentServiceProviderSEP.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInitResponseDTO {
    private String paymentUrl;
    private String paymentId;
    private String message;
}
