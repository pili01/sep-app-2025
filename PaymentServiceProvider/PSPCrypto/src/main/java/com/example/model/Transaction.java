package com.example.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String pspTransactionId;

    @Column(unique = true)
    private String globalTransactionId; // Bitcoin transaction hash (kada se primi payment)

    @Column(nullable = false)
    private BigDecimal amount; // Fiat amount

    @Column(nullable = false, length = 3)
    private String currency; // Fiat currency

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(nullable = false)
    private String webhookUrl;

    @Column(nullable = false)
    @NotBlank(message = "Success URL is required")
    private String successUrl;

    @Column(nullable = false)
    @NotBlank(message = "Failed URL is required")
    private String failedUrl;

    @Column(nullable = false)
    @NotBlank(message = "Error URL is required")
    private String errorUrl;

    // Crypto specific fields
    @Column
    private String bitcoinAddress;

    @Column(precision = 18, scale = 8)
    private BigDecimal bitcoinAmount;

    @Column
    private Integer confirmations = 0;

    @Column
    private Integer requiredConfirmations = 1;

    @Column
    private LocalDateTime paymentReceivedAt;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime completedAt;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean pspNotified = false;
}
