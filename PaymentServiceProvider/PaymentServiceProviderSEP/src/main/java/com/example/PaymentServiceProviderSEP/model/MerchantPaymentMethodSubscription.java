package com.example.PaymentServiceProviderSEP.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table()
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MerchantPaymentMethodSubscription {
    // Pretplata web shopa na određenu metodu plaćanja
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    @NotNull(message = "Merchant is required")
    private Merchant merchant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Payment method code is required")
    private PaymentMethodCode paymentMethodCode;

    @Column(nullable = false)
    @NotNull(message = "Enabled status is required")
    private Boolean enabled = true;

    @Column(columnDefinition = "TEXT")
    private String configJson; // npr. paypal clientId/secret, crypto xpub, itd

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

