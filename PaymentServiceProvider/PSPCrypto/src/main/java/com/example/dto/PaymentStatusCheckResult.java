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

    private boolean paymentFound; //ima li transakcija na blokcejnu

    private String transactionHash;

    private Integer confirmations;

    private boolean confirmed;

    private BigDecimal receivedAmount;  //iznos koji jje stigao

    private BigDecimal expectedAmount;  // ocekivani iznos

    private boolean amountMatches;     //da li iznos odgovara ocekivnom

    private LocalDateTime receivedAt;

    private LocalDateTime confirmedAt;

    private String message;
}
