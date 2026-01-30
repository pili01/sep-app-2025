package com.example.WebShopSEP.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SoftDelete;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "rentals")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@ToString(exclude = {"user", "vehicle", "insurance", "equipment"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@SoftDelete
public class Rental {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    @Column(name = "end_date", nullable = false)
    private Instant endDate;

    @Column(name = "total_price", nullable = false)
    private Double totalPrice;

    @ManyToOne
    @JoinColumn(name = "insurance_id")
    private Insurance insurance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RentalStatus status = RentalStatus.DRAFT;

    @Column(name = "payment_method")
    private String paymentMethod;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "rental_equipment",
            joinColumns = @JoinColumn(name = "rental_id"),
            inverseJoinColumns = @JoinColumn(name = "equipment_id")
    )
    private Set<Equipment> equipment = new HashSet<>();

    public void calculateTotalPrice() {
        if (startDate != null && endDate != null) {
            long days = java.time.Duration.between(startDate, endDate).toDays();
            if (days < 1) days = 1;

            double total = 0.0;

            if (vehicle != null) {
                total += days * vehicle.getPricePerDay();
            }

            if (insurance != null) {
                total += days * insurance.getPricePerDay();
            }

            if (equipment != null && !equipment.isEmpty()) {
                for (Equipment eq : equipment) {
                    total += days * eq.getPricePerDay();
                }
            }

            this.totalPrice = total;
        }
    }

    @PrePersist
    @PreUpdate
    private void validateAndCalculate() {
        if (endDate != null && startDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }
        calculateTotalPrice();
    }

    public void addEquipment(Equipment equipmentItem) {
        this.equipment.add(equipmentItem);
        calculateTotalPrice();
    }

    public void removeEquipment(Equipment equipmentItem) {
        this.equipment.remove(equipmentItem);
        calculateTotalPrice();
    }
}