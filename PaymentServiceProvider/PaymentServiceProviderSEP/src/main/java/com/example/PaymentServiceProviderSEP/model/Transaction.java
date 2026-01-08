package com.example.PaymentServiceProviderSEP.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SoftDelete;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
@Table
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SoftDelete
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private String merchantId;

    @Column(nullable = false, updatable = false)
    private Double amount;

    @Column(nullable = false, updatable = false)
    private String currency;

    /// id koji se dobije od banke za odredjeni web shop
    @Column(nullable = false, updatable = false)
    private String merchantIdFromBank;

    @Column(nullable = false, unique = true, updatable = false)
    private String transactionId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status = TransactionStatus.INITIALIZED;

    @Column(nullable = false, updatable = false)
    private Timestamp pspTimestamp;

    @Column
    @Enumerated(EnumType.STRING)
    private PaymentMethodCode paymentMethod;

    @Column(nullable = false, unique = true, updatable = false)
    private String STAN;

    @Column
    private String globalTransactionId; // od banke

    @Column
    private LocalDateTime acquirerTimestamp; // od banke

    public Transaction(String merchantId, Double amount, String currency, String merchantIdFromBank, String transactionId) {
        this.merchantId = merchantId;
        this.amount = amount;
        this.currency = currency;
        this.merchantIdFromBank = merchantIdFromBank;
        this.transactionId = transactionId;
        this.pspTimestamp = new Timestamp(System.currentTimeMillis());
        this.paymentMethod = null;
        this.STAN = merchantIdFromBank + pspTimestamp;

    }
}
