package com.example.WebShopSEP.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SoftDelete;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "insurances")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SoftDelete
@ToString(exclude = "rentals")
public class Insurance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company", nullable = false)
    private String company;

    @Column(name = "name")
    private String name;

    @Column(name = "price_per_day", nullable = false)
    private Double pricePerDay;

    @JsonIgnore
    @OneToMany(mappedBy = "insurance", cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    private List<Rental> rentals = new ArrayList<>();
}