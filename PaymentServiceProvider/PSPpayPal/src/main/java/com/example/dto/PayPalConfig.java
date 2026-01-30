package com.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayPalConfig {

    @JsonProperty("clientId")
    private String clientId;

    @JsonProperty("secret")
    private String secret;

    @JsonProperty("mode")
    private String mode; // sandbox or live
}
