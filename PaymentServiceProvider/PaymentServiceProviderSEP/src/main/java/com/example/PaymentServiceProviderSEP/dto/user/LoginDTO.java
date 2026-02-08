package com.example.PaymentServiceProviderSEP.dto.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginDTO {
    private String email;
    private String password;
}