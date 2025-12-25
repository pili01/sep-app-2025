package com.example.PaymentServiceProviderSEP.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Merchants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Merchant {
    //Pretplata web shopa na psp
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PSP internal ID

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Merchant ID is required")
    private String merchantId; // Ono što web shop dobije pri pretplati

    @Column(nullable = false)
    @NotBlank(message = "Merchant password/API key is required")
    private String merchantPassword; // Za autentifikaciju zahteva (apiKey)

    @Column(nullable = false)
    @NotBlank(message = "Merchant name is required")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Merchant status is required")
    private MerchantStatus status = MerchantStatus.DRAFT;

    @Column(nullable = false)
    @NotBlank(message = "Success URL is required")
    private String successUrl;

    @Column(nullable = false)
    @NotBlank(message = "Failed URL is required")
    private String failedUrl;

    @Column(nullable = false)
    @NotBlank(message = "Error URL is required")
    private String errorUrl;
}

