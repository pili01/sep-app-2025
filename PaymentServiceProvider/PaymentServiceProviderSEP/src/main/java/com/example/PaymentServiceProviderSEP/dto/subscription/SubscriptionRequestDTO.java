package com.example.PaymentServiceProviderSEP.dto.subscription;

import com.example.PaymentServiceProviderSEP.model.PaymentMethodCode;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubscriptionRequestDTO {
    private String merchantAccountNumber;
    private String configJson;
    private  Long paymentMethodId;
}
