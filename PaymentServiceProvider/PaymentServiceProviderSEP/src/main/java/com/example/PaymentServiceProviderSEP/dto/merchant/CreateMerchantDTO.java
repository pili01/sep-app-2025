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
public class CreateMerchantDTO {
    @NotBlank
    private String name;

    @NotBlank
    private String successUrl;

    @NotBlank
    private String failedUrl;

    @NotBlank
    private String errorUrl;
}
