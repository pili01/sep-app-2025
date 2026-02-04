package com.example.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO za Blockstream API Status objekat
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlockstreamStatus {
    @JsonProperty("confirmed")
    private Boolean confirmed; // Da li je transakcija potvrđena
    
    @JsonProperty("block_height")
    private Integer blockHeight; // Block height (null ako nije potvrđen)
    
    @JsonProperty("block_hash")
    private String blockHash; // Block hash
    
    @JsonProperty("block_time")
    private Long blockTime; // Unix timestamp kada je block kreiran
}
