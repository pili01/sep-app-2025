package com.example.PaymentServiceProviderSEP.dto.paymentMethod;

import com.example.PaymentServiceProviderSEP.model.PaymentMethodCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodConnectRequest {

    private String name;
    private String hostname;
    private String statusUrl;
    private String paymentUrl;
    private String paymentMethodCode;
}