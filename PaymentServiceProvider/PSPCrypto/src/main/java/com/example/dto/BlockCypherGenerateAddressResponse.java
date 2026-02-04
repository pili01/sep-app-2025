package com.example.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO za BlockCypher API response kada se generiše nova Bitcoin adresa
 * POST /v1/btc/test3/addrs
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlockCypherGenerateAddressResponse {
    
    @JsonProperty("address")
    private String address; // Nova generisana Bitcoin adresa
    
    @JsonProperty("private")
    private String privateKey; // Private key (samo za testnet, ne koristimo ga)
    
    @JsonProperty("public")
    private String publicKey; // Public key (ne koristimo ga)
    
    @JsonProperty("wif")
    private String wif; // Wallet Import Format (ne koristimo ga)
}
