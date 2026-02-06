package com.example.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "address_index")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddressIndex {

    @Id
    private Long id = 1L;

    @Column(nullable = false)
    private Integer lastUsedIndex = 0; // Poslednji korišćeni indeks za HD wallet derivaciju
}
