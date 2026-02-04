package com.example.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO za BlockCypher Transaction Input
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlockCypherInput {
    @JsonProperty("prev_hash")
    private String prevHash; // Hash prethodne transakcije
    
    @JsonProperty("output_index")
    private Integer outputIndex; // Index output-a
    
    @JsonProperty("script")
    private String script; // Script
}
