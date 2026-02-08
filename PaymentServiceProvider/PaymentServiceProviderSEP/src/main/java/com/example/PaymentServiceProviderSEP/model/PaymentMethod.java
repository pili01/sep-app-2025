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
@Table(name = "PaymentMethods")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String hostname;

    @Column(nullable = false)
    private String healthEndpoint;

    @Column(nullable = false)
    private String paymentEndpoint;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private boolean enabled;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime lastHeartbeat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Payment method code is required")
    private PaymentMethodCode paymentMethodCode = PaymentMethodCode.CUSTOM;

    @Column
    private String iconPath;
}
