package com.example.WebShopSEP.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SoftDelete;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vehicles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SoftDelete
@ToString(exclude = "rentals")
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @JsonIgnore
    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Rental> rentals = new ArrayList<>();
}