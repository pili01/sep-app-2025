package com.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CryptoConfig {

    @JsonProperty("walletAddress")
    private String walletAddress;        // bilo koja adresa ako ovi apiji ne rade da na ovo usmjeri

    @JsonProperty("blockcypherApiToken")
    private String blockcypherApiToken; // ovo ide opciono ako os za test je prazan

    @JsonProperty("network")
    private String network; // testnet

    @JsonProperty("requiredConfirmations")
    private Integer requiredConfirmations = 1; // jedna potvrda mi je dosta
}
