package com.example.WebShopSEP.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SoftDelete;

import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Table
@Getter
@Setter
@SoftDelete
@AllArgsConstructor @NoArgsConstructor
@ToString(exclude = "rentals")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Transaction {
    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private String transactionId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(nullable = false)
    private Timestamp timestamp;

    @Column
    private String paymentMethod;

    @Column(nullable = false)
    private Long rentalId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Transaction(User user, Long rentalId) {
        this.timestamp = new Timestamp(System.currentTimeMillis());
        this.transactionId = UUID.randomUUID().toString();
        this.user = user;
        this.rentalId = rentalId;
    }
}
