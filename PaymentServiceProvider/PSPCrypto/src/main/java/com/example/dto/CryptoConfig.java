package com.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CryptoConfig {

    @JsonProperty("xpub")
    private String xpub; // Master public key (Extended Public Key) za HD wallet generisanje adresa

    @JsonProperty("walletAddress")
    private String walletAddress;

    @JsonProperty("network")
    private String network;

    @JsonProperty("requiredConfirmations")
    private Integer requiredConfirmations = 1; // jedna potvrda mi je dosta
}
