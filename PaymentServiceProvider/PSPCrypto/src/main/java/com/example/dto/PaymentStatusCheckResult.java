package com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Rezultat provere payment statusa na blockchain-u
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusCheckResult {
    /**
     * Da li je payment pronađen na blockchain-u
     */
    private boolean paymentFound;
    
    /**
     * Transaction hash (ako je payment pronađen)
     */
    private String transactionHash;
    
    /**
     * Broj potvrda (confirmations)
     */
    private Integer confirmations;
    
    /**
     * Da li je payment potvrđen (ima dovoljno potvrda)
     */
    private boolean confirmed;
    
    /**
     * Iznos koji je stigao na adresu (u BTC)
     */
    private BigDecimal receivedAmount;
    
    /**
     * Očekivani iznos (u BTC)
     */
    private BigDecimal expectedAmount;
    
    /**
     * Da li iznos odgovara očekivanom (sa tolerancijom)
     */
    private boolean amountMatches;
    
    /**
     * Timestamp kada je payment primljen (ako je pronađen)
     */
    private LocalDateTime receivedAt;
    
    /**
     * Timestamp kada je payment potvrđen (ako je potvrđen)
     */
    private LocalDateTime confirmedAt;
    
    /**
     * Poruka o statusu (za logovanje)
     */
    private String message;
}
