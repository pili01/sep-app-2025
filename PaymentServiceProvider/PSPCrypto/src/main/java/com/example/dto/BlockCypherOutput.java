package com.example.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlockCypherOutput {
    @JsonProperty("value")
    private Long value;
    
    @JsonProperty("addresses")
    private List<String> addresses;
    
    @JsonProperty("script")
    private String script;

    public BigDecimal getValueInBTC() {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(value).divide(BigDecimal.valueOf(100_000_000), 8, java.math.RoundingMode.HALF_UP);
    }
}
