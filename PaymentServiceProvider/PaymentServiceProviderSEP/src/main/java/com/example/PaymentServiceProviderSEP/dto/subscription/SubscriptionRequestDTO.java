package com.example.PaymentServiceProviderSEP.dto.subscription;

import com.example.PaymentServiceProviderSEP.model.PaymentMethodCode;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubscriptionRequestDTO {
    private PaymentMethodCode paymentMethodCode;
    private String merchantAccountNumber;
    private String configJson;

    @AssertTrue(message = "merchantAccountNumber must be provided when paymentMethodCode is BANK_CARD")
    public boolean isValid() {
        if(paymentMethodCode == PaymentMethodCode.BANK_CARD) {
            return merchantAccountNumber != null && !merchantAccountNumber.isEmpty();
        }
        else return true;
    }
}
