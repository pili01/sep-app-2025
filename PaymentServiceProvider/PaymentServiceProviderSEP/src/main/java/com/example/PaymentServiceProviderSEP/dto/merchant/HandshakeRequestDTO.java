package com.example.PaymentServiceProviderSEP.dto.merchant;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HandshakeRequestDTO {
    @NotBlank
    private String merchantId;

    @NotBlank
    private String merchantPassword;
}
