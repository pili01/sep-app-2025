package com.example.PaymentServiceProviderSEP.dto.paymentMethod;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentMethodDTO {

    private Long id;
    private String name;
    private String paymentMethodCode;
    private Boolean active;
    private LocalDateTime lastHeartbeat;
//    private String hostname;
//    private String healthEndpoint;
//    private String paymentEndpoint;
}