package com.example.WebShopSEP.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SoftDelete;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "equipment")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@ToString(exclude = {"rentals"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@SoftDelete
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "price_per_day", nullable = false)
    private Double pricePerDay;

    @JsonIgnore
    @ManyToMany(mappedBy = "equipment", fetch = FetchType.LAZY)
    private Set<Rental> rentals = new HashSet<>();
}