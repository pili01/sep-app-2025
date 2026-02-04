package com.example.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlockstreamOutput {
    @JsonProperty("value")
    private Long value;
    
    @JsonProperty("scriptpubkey_address")
    private String scriptpubkeyAddress;
    

    public BigDecimal getValueInBTC() {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(value).divide(BigDecimal.valueOf(100_000_000), 8, java.math.RoundingMode.HALF_UP);
    }
}
