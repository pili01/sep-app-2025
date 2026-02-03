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
    private String walletAddress; // Merchant wallet adresa

    @JsonProperty("network")
    private String network; // "testnet" ili "mainnet"

    @JsonProperty("requiredConfirmations")
    private Integer requiredConfirmations = 1; // Koliko potvrda treba (default 1)
}
