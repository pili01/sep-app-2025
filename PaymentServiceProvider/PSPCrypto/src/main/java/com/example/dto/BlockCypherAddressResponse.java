package com.example.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO za BlockCypher API response kada se proverava adresa SA TRANSAKCIJAMA
 * GET /v1/btc/test3/addrs/{address}/full?limit=50
 * Koristimo /full endpoint da dobijemo sve transakcije sa detaljima
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlockCypherAddressResponse {
    @JsonProperty("address")
    private String address;
    
    @JsonProperty("balance")
    private Long balance; // Balance u satoshima (1 BTC = 100,000,000 satoshi)
    
    @JsonProperty("total_received")
    private Long totalReceived; // Ukupno primljeno u satoshima
    
    @JsonProperty("total_sent")
    private Long totalSent; // Ukupno poslato u satoshima
    
    @JsonProperty("unconfirmed_balance")
    private Long unconfirmedBalance; // Ne potvrđen balance u satoshima
    
    @JsonProperty("final_balance")
    private Long finalBalance; // Finalni balance u satoshima
    
    @JsonProperty("n_tx")
    private Integer nTx; // Broj transakcija
    
    @JsonProperty("unconfirmed_n_tx")
    private Integer unconfirmedNTx; // Broj nepotvrđenih transakcija
    
    @JsonProperty("txs")
    private List<BlockCypherTransaction> transactions; // Lista transakcija
    
    /**
     * Konvertuje balance iz satoshi u BTC
     */
    public BigDecimal getBalanceInBTC() {
        if (balance == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(balance).divide(BigDecimal.valueOf(100_000_000), 8, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Konvertuje final balance iz satoshi u BTC
     */
    public BigDecimal getFinalBalanceInBTC() {
        if (finalBalance == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(finalBalance).divide(BigDecimal.valueOf(100_000_000), 8, java.math.RoundingMode.HALF_UP);
    }
}
