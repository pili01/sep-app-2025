package com.example.WebShopSEP.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vehicles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "rentals")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "price_per_day", nullable = false)
    private Double pricePerDay;

    @Column(unique = true, nullable = false)
    private String registration;

    @Column(name = "chassis_number", unique = true, nullable = false)
    private String chassisNumber;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "picture_url")
    private String pictureUrl;

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Rental> rentals = new ArrayList<>();
}