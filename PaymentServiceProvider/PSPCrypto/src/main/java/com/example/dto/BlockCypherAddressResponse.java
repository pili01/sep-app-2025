package com.example.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlockCypherAddressResponse {
    @JsonProperty("address")
    private String address;
    
    @JsonProperty("balance")
    private Long balance;
    
    @JsonProperty("total_received")
    private Long totalReceived;
    
    @JsonProperty("total_sent")
    private Long totalSent;
    
    @JsonProperty("unconfirmed_balance")
    private Long unconfirmedBalance;

    @JsonProperty("final_balance")
    private Long finalBalance;
    
    @JsonProperty("n_tx")
    private Integer nTx; // Broj transakcija
    
    @JsonProperty("unconfirmed_n_tx")
    private Integer unconfirmedNTx; // Broj nepotvrđenih transakcija
    
    @JsonProperty("txs")
    private List<BlockCypherTransaction> transactions;
    

    public BigDecimal getBalanceInBTC() {
        if (balance == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(balance).divide(BigDecimal.valueOf(100_000_000), 8, java.math.RoundingMode.HALF_UP);
    }
    

    public BigDecimal getFinalBalanceInBTC() {
        if (finalBalance == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(finalBalance).divide(BigDecimal.valueOf(100_000_000), 8, java.math.RoundingMode.HALF_UP);
    }
}
