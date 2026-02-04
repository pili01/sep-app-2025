package com.example.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlockstreamStatus {
    @JsonProperty("confirmed")
    private Boolean confirmed;
    
    @JsonProperty("block_height")
    private Integer blockHeight; //null ako nije potvrdjena
    
    @JsonProperty("block_hash")
    private String blockHash;
    
    @JsonProperty("block_time")
    private Long blockTime;
}
