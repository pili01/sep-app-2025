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
    private String walletAddress; // Opciono: koristi se kao fallback ako BlockCypher API ne radi, ili za backward compatibility (stara logika bez BlockCypher API)

    @JsonProperty("blockcypherApiToken")
    private String blockcypherApiToken; // BlockCypher API token (opciono - za testnet može biti prazan string "")

    @JsonProperty("network")
    private String network; // "testnet" ili "mainnet"

    @JsonProperty("requiredConfirmations")
    private Integer requiredConfirmations = 1; // Koliko potvrda treba (default 1)
}
