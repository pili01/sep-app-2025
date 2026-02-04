package com.example.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlockCypherGenerateAddressResponse {
    
    @JsonProperty("address")
    private String address;  //samo adresa mi treba generisana za ovim apijem
    
    @JsonProperty("private")
    private String privateKey; //ne koristim za testnet
    
    @JsonProperty("public")
    private String publicKey; //ne koristim za testnet
    
    @JsonProperty("wif")
    private String wif; //ne koristim za testnet
}
