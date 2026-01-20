package com.example.PaymentServiceProviderSEP.dto.subscription;

import com.example.PaymentServiceProviderSEP.model.PaymentMethodCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionResponseDTO {
    private Long id;
    private Long merchantId;
    private Long paymentMethodId;
    private String paymentMethodName;
    private PaymentMethodCode paymentMethodCode;
    private Boolean enabled;
    private Boolean active;
    private String configJson;
    private LocalDateTime createdAt;
}
