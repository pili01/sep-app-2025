package com.example.WebShopSEP.dto.rental;

import com.example.WebShopSEP.dto.equipment.EquipmentResponseDTO;
import com.example.WebShopSEP.dto.insurance.InsuranceResponseDTO;
import com.example.WebShopSEP.dto.user.UserDto;
import com.example.WebShopSEP.dto.vehicle.VehicleResponseDTO;
import com.example.WebShopSEP.model.RentalStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RentalDto {

    private Long id;

    private UserDto user;

    private VehicleResponseDTO vehicle;

    @NotNull(message = "Vehicle ID cannot be null")
    private Long vehicleId;

    @NotNull(message = "Start date cannot be null")
    @FutureOrPresent(message = "Start date must be in the present or future")
    private Instant startDate;

    @NotNull(message = "End date cannot be null")
    @FutureOrPresent(message = "End date must be in the present or future")
    private Instant endDate;

    private Double totalPrice;

    private InsuranceResponseDTO insurance;

    private Long insuranceId;

    private RentalStatus status = RentalStatus.DRAFT;

    private Set<EquipmentResponseDTO> equipment = new HashSet<>();

    @AssertTrue(message = "End date must be after start date")
    private boolean isEndDateAfterStartDate() {
        if (startDate == null || endDate == null) {
            return true; // Handled by @NotNull
        }
        return endDate.isAfter(startDate) || endDate.equals(startDate);
    }
}
