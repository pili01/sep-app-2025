package com.example.WebShopSEP.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "insurances")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "rentals")
public class Insurance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "company", nullable = false)
    private String company;

    @Column(name = "name")
    private String name;

    @Column(name = "price_per_day", nullable = false)
    private Double pricePerDay;

    @OneToMany(mappedBy = "insurance", cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    private List<Rental> rentals = new ArrayList<>();
}