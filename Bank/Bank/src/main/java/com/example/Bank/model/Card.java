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
    private String cardNumber;

    @Column(nullable = false)
    private String cardholderName;

    @Column(nullable = false)
    private String expirationDate;

    @Column(nullable = false)
    private String cvv;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Account account;


    public boolean isValid() {
        if (cardNumber == null || !cardNumber.matches("\\d+")) {
            return false;
        }

        int sum = 0;
        boolean doubleDigit = false;

        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = cardNumber.charAt(i) - '0';

            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            doubleDigit = !doubleDigit;
        }

        return sum % 10 == 0;
    }

}
