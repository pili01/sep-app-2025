package com.example.WebShopSEP.service;

import com.example.WebShopSEP.dto.rental.RentalDto;
import com.example.WebShopSEP.model.*;
import com.example.WebShopSEP.repository.*;
import jakarta.validation.Valid;
import org.hibernate.Hibernate;
import org.jspecify.annotations.Nullable;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class RentalService {
    public final RentalRepository rentalRepository;
    public final UserRepository userRepository;
    public final EquipmentRepository equipmentRepository;
    public final VehicleRepository vehicleRepository;
    public final InsuranceRepository insuranceRepository;
    public final ModelMapper modelMapper;

    public RentalService(RentalRepository rentalRepository, ModelMapper modelMapper, UserRepository userRepository, EquipmentRepository equipmentRepository, VehicleRepository vehicleRepository, InsuranceRepository insuranceRepository) {
        this.rentalRepository = rentalRepository;
        this.modelMapper = modelMapper;
        this.userRepository = userRepository;
        this.equipmentRepository = equipmentRepository;
        this.vehicleRepository = vehicleRepository;
        this.insuranceRepository = insuranceRepository;
    }

    @Transactional(readOnly = false)
    public String createRental(@Valid RentalDto rentalDto, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Vehicle vehicle = vehicleRepository.findById(rentalDto.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        Insurance insurance = insuranceRepository.findById(rentalDto.getInsuranceId())
                .orElseThrow(() -> new RuntimeException("Insurance not found"));
        Rental rental = modelMapper.map(rentalDto, Rental.class);
        rental.setUser(user);
        rental.setVehicle(vehicle);
        rental.setInsurance(insurance);
        Set<Equipment> equipmentSet = new HashSet<>();
        for (Integer equipmentId : rentalDto.getEquipmentIds()) {
            Equipment equipment = equipmentRepository.findById(equipmentId)
                    .orElseThrow(() -> new RuntimeException("Equipment with ID " + equipmentId + " not found"));
            equipmentSet.add(equipment);
        }
        rental.setEquipment(equipmentSet);
        rental.calculateTotalPrice();
        rentalRepository.save(rental);
        return "Rental created successfully";
    }

    @Transactional(readOnly = true)
    public RentalDto getRentalById(Integer id) {
        Rental rental = rentalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rental not found"));
        return modelMapper.map(rental, RentalDto.class);
    }

    @Transactional(readOnly = true)
    public List<RentalDto> getAllMyRentals(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Rental> rentals = rentalRepository.findByUser(user);
        List<RentalDto> rentalDtos = new ArrayList<>();
        for (Rental rental : rentals) {
            rentalDtos.add(modelMapper.map(rental, RentalDto.class));
        }
        return rentalDtos;
    }

    @Transactional(readOnly = true)
    public List<RentalDto> getAllRentals() {
        List<Rental> rentals = rentalRepository.findAllForAuthor();
        List<RentalDto> rentalDtos = new ArrayList<>();
        for (Rental rental : rentals) {
            rentalDtos.add(modelMapper.map(rental, RentalDto.class));
        }
        return rentalDtos;
    }

    @Transactional(readOnly = false)
    public String cancelRental(Integer id, String email) {
        Rental rental = rentalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rental not found"));

        // Proveri da li korisnik ima pravo da otkaže (samo vlasnik ili AUTHOR)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Proveri da li je DRAFT status
        if (rental.getStatus() != RentalStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT rentals can be cancelled");
        }

        // Proveri da li je korisnik vlasnik ili AUTHOR
        if (!rental.getUser().getEmail().equals(email) && !user.getRole().name().equals("AUTHOR")) {
            throw new RuntimeException("You don't have permission to cancel this rental");
        }

        rentalRepository.delete(rental);
        return "Rental cancelled successfully";
    }
}
