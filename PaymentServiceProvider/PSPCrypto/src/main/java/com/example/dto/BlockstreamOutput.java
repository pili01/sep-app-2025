package com.example.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;

/**
 * DTO za Blockstream API Transaction Output
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlockstreamOutput {
    @JsonProperty("value")
    private Long value; // Iznos u satoshima
    
    @JsonProperty("scriptpubkey_address")
    private String scriptpubkeyAddress; // Bitcoin adresa
    
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
