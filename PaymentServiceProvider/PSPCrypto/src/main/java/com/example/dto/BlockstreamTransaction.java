package com.example.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO za Blockstream API Transaction objekat
 * GET https://blockstream.info/testnet/api/tx/{txid}
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlockstreamTransaction {
    @JsonProperty("txid")
    private String txid; // Transaction hash
    
    @JsonProperty("status")
    private BlockstreamStatus status; // Status sa confirmations
    
    @JsonProperty("fee")
    private Long fee; // Transaction fee u satoshima
    
    @JsonProperty("vout")
    private List<BlockstreamOutput> vout; // Output transakcije
    
    /**
     * Konvertuje fee iz satoshi u BTC
     */
    public BigDecimal getFeeInBTC() {
        if (fee == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(fee).divide(BigDecimal.valueOf(100_000_000), 8, java.math.RoundingMode.HALF_UP);
    }
    
}
