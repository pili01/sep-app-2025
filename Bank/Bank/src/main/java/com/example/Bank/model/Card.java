package com.example.Bank.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLDelete;

@Entity
@Table()
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(unique = true, nullable = false)
    private String panEnc;

    @Column(unique = true, nullable = false)
    private String panHash;

    @Column(nullable = false)
    private String cardholderName;

    @Column(nullable = false)
    private String expirationDate;

    @Column(nullable = false)
    private String cvvEnc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Account account;
}
