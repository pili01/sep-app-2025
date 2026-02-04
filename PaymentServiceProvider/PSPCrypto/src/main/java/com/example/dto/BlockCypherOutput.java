package com.example.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO za BlockCypher Transaction Output
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlockCypherOutput {
    @JsonProperty("value")
    private Long value; // Iznos u satoshima
    
    @JsonProperty("addresses")
    private List<String> addresses; // Lista adresa koje primaju
    
    @JsonProperty("script")
    private String script; // Script
    
    /**
     * Konvertuje value iz satoshi u BTC
     */
    public BigDecimal getValueInBTC() {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(value).divide(BigDecimal.valueOf(100_000_000), 8, java.math.RoundingMode.HALF_UP);
    }
}
