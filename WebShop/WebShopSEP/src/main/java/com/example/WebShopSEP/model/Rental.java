package com.example.WebShopSEP.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "rentals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"user", "vehicle", "insurance", "equipment"})
public class Rental {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "total_price", nullable = false)
    private Double totalPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurance_id")
    private Insurance insurance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RentalStatus status = RentalStatus.DRAFT;

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
        equipmentItem.getRentals().add(this);
        calculateTotalPrice();
    }

    public void removeEquipment(Equipment equipmentItem) {
        this.equipment.remove(equipmentItem);
        equipmentItem.getRentals().remove(this);
        calculateTotalPrice();
    }

    public Rental(User user, Vehicle vehicle, Insurance insurance,
                  LocalDateTime startDate, LocalDateTime endDate) {
        this.user = user;
        this.vehicle = vehicle;
        this.insurance = insurance;
        this.startDate = startDate;
        this.endDate = endDate;
        this.equipment = new HashSet<>();
        calculateTotalPrice();
    }
}