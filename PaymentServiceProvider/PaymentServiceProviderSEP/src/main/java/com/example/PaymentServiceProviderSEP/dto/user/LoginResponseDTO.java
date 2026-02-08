package com.example.PaymentServiceProviderSEP.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class LoginResponseDTO {
    private String token;
    private Integer userId;
    private String email;
    private String userRole;
}