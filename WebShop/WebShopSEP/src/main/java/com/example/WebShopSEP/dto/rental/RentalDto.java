package com.example.WebShopSEP.dto.rental;

import com.example.WebShopSEP.dto.equipment.EquipmentResponseDTO;
import com.example.WebShopSEP.dto.insurance.InsuranceResponseDTO;
import com.example.WebShopSEP.model.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RentalDto {

    private Integer id;

    private User user;

    private Vehicle vehicle;

    @NotNull(message = "Vehicle ID cannot be null")
    private Integer vehicleId;

    @NotNull(message = "Start date cannot be null")
    @FutureOrPresent(message = "Start date must be in the present or future")
    private LocalDateTime startDate;

    @NotNull(message = "End date cannot be null")
    @FutureOrPresent(message = "End date must be in the present or future")
    private LocalDateTime endDate;

    private Double totalPrice;

    private InsuranceResponseDTO insurance;

    private Integer insuranceId;

    private RentalStatus status = RentalStatus.DRAFT;

    private Set<EquipmentResponseDTO> equipment = new HashSet<>();

    private Set<Integer> equipmentIds = new HashSet<>();

    @AssertTrue(message = "End date must be after start date")
    private boolean isEndDateAfterStartDate() {
        if (startDate == null || endDate == null) {
            return true; // Handled by @NotNull
        }
        return endDate.isAfter(startDate) || endDate.isEqual(startDate);
    }
}
