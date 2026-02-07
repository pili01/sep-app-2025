package com.example.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlockCypherInput {
    @JsonProperty("prev_hash")
    private String prevHash;
    
    @JsonProperty("output_index")
    private Integer outputIndex;
    
    @JsonProperty("script")
    private String script;
}
