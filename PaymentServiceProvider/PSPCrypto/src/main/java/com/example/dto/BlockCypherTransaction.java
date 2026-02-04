package com.example.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO za BlockCypher Transaction objekat
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlockCypherTransaction {
    @JsonProperty("hash")
    private String hash; // Transaction hash
    
    @JsonProperty("block_height")
    private Integer blockHeight; // Block height (null ako nije potvrđen)
    
    @JsonProperty("confirmations")
    private Integer confirmations; // Broj potvrda
    
    @JsonProperty("confirmed")
    private String confirmed; // ISO 8601 timestamp kada je potvrđen
    
    @JsonProperty("received")
    private String received; // ISO 8601 timestamp kada je primljen
    
    @JsonProperty("total")
    private Long total; // Ukupan iznos u satoshima
    
    @JsonProperty("fees")
    private Long fees; // Transaction fees u satoshima
    
    @JsonProperty("inputs")
    private List<BlockCypherInput> inputs; // Input transakcije
    
    @JsonProperty("outputs")
    private List<BlockCypherOutput> outputs; // Output transakcije
    
    /**
     * Konvertuje total iz satoshi u BTC
     */
    public BigDecimal getTotalInBTC() {
        if (total == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(total).divide(BigDecimal.valueOf(100_000_000), 8, java.math.RoundingMode.HALF_UP);
    }
}
