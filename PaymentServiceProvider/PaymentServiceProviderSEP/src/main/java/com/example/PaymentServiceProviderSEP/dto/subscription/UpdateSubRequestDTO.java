package com.example.PaymentServiceProviderSEP.dto.subscription;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateSubRequestDTO {
    private Boolean enabled;
    private String configJson;
}
