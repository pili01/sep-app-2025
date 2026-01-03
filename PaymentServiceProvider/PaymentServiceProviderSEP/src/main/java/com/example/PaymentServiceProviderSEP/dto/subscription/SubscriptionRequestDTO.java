package com.example.PaymentServiceProviderSEP.dto.subscription;

import com.example.PaymentServiceProviderSEP.model.PaymentMethodCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubscriptionRequestDTO {
    private PaymentMethodCode paymentMethodCode;
    private String configJson;
}
