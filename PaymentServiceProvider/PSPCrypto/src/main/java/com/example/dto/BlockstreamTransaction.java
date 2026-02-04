package com.example.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlockstreamTransaction {
    @JsonProperty("txid")
    private String txid; // hes od transakcije
    
    @JsonProperty("status")
    private BlockstreamStatus status;
    
    @JsonProperty("fee")
    private Long fee;
    
    @JsonProperty("vout")
    private List<BlockstreamOutput> vout;


    public BigDecimal getFeeInBTC() {
        if (fee == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(fee).divide(BigDecimal.valueOf(100_000_000), 8, java.math.RoundingMode.HALF_UP);
    }
    
}
