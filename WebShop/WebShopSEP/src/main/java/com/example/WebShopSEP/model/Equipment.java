package com.example.WebShopSEP.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "equipment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "rentals")
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "price_per_day", nullable = false)
    private Double pricePerDay;

    @ManyToMany(mappedBy = "equipment")
    private Set<Rental> rentals = new HashSet<>();
}