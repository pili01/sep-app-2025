package com.example.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlockCypherTransaction {
    @JsonProperty("hash")
    private String hash;
    
    @JsonProperty("block_height")
    private Integer blockHeight;
    
    @JsonProperty("confirmations")
    private Integer confirmations;
    
    @JsonProperty("confirmed")
    private String confirmed;
    
    @JsonProperty("received")
    private String received;
    
    @JsonProperty("total")
    private Long total;
    
    @JsonProperty("fees")
    private Long fees;
    
    @JsonProperty("inputs")
    private List<BlockCypherInput> inputs; // input transakcije
    
    @JsonProperty("outputs")
    private List<BlockCypherOutput> outputs; // output transakcije
    

    public BigDecimal getTotalInBTC() {
        if (total == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(total).divide(BigDecimal.valueOf(100_000_000), 8, java.math.RoundingMode.HALF_UP);
    }
}
