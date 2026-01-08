package com.example.Bank.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String paymentId; //ovo ce ici za onaj payment url

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String currency; // valutaa

    @Column(nullable = false)
    private String merchantName;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Boolean used = false; // da li je forma koristena jer ima jedan pokusaj

    @Column
    private String stan; // stan od pspa

    @Column
    private String globalTransactionId;

    @Column
    private LocalDateTime acquirerTimestamp; // timestamp kada je bbanka obradila transakciju

    @Column
    private String successUrl; // URL za redirekciju nakon uspešnog plaćanja

    @Column
    private String failedUrl; // URL za redirekciju nakon neuspešnog plaćanja

    @Column
    private String errorUrl; // URL za redirekciju u slučaju greške
}

