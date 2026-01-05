package com.example.WebShopSEP.service;

import com.example.WebShopSEP.config.ConfigProperties;
import com.example.WebShopSEP.dto.equipment.EquipmentResponseDTO;
import com.example.WebShopSEP.dto.rental.RentalDto;
import com.example.WebShopSEP.model.*;
import com.example.WebShopSEP.modelMapper.RentalModelMapper;
import com.example.WebShopSEP.repository.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Transactional
@RequiredArgsConstructor
public class RentalService {
    public final RentalRepository rentalRepository;
    public final UserRepository userRepository;
    public final EquipmentRepository equipmentRepository;
    public final VehicleRepository vehicleRepository;
    public final InsuranceRepository insuranceRepository;
    private final TransactionRepository transactionRepository;
    private final RentalModelMapper rentalModelMapper;
    private final PSPClientService pspClientService;

    @Transactional(readOnly = false)
    public String createRental(@Valid RentalDto rentalDto, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Vehicle vehicle = vehicleRepository.findById(rentalDto.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        Insurance insurance = insuranceRepository.findById(rentalDto.getInsuranceId())
                .orElseThrow(() -> new RuntimeException("Insurance not found"));

        Rental rental = new Rental();
        rental.setUser(user);
        rental.setVehicle(vehicle);
        rental.setInsurance(insurance);
        rental.setStartDate(rentalDto.getStartDate());
        rental.setEndDate(rentalDto.getEndDate());
        rental.setStatus(RentalStatus.DRAFT);

        if (rentalDto.getEquipment() != null && !rentalDto.getEquipment().isEmpty()) {
            for (EquipmentResponseDTO eqDto : rentalDto.getEquipment()) {
                if (eqDto.getId() > 0) {
                    Equipment equipment = equipmentRepository.findById(eqDto.getId())
                            .orElseThrow(() -> new RuntimeException("Equipment with ID " + eqDto.getId() + " not found"));
                    rental.addEquipment(equipment);
                }
            }
        }

        Rental savedRental = rentalRepository.save(rental);
        return "Rental created successfully with ID: " + savedRental.getId();
    }

    @Transactional(readOnly = true)
    public RentalDto getRentalById(Long id) {
        Rental rental = rentalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rental not found"));
        return rentalModelMapper.toDto(rental);
    }

    @Transactional(readOnly = true)
    public List<RentalDto> getAllMyRentals(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Rental> rentals = rentalRepository.findByUser(user.getId());
        return rentalModelMapper.toDtoSet(rentals);
    }

    @Transactional(readOnly = true)
    public List<RentalDto> getAllRentals() {
        List<Rental> rentals = rentalRepository.findAll();
        return rentalModelMapper.toDtoSet(rentals);
    }

    @Transactional(readOnly = false)
    public String cancelRental(Long id, String email) {
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

    public String payForRental(Long id, String email) {
        Rental rental = rentalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rental not found"));
        if (!rental.getUser().getEmail().equals(email)) {
            throw new RuntimeException("You don't have permission to pay for this rental");
        }
        if (rental.getStatus() != RentalStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT rentals can be paid for");
        }

        Transaction transaction = new Transaction(rental.getUser(), rental.getId());
        while (transactionRepository.existsByTransactionId(transaction.getTransactionId())) {
            transaction = new Transaction(rental.getUser(), rental.getId());
        }
        Transaction savedTransaction = transactionRepository.save(transaction);

        return pspClientService.initializePayment(
                rental.getTotalPrice(),
                savedTransaction.getTransactionId(),
                savedTransaction.getTimestamp().toString()
        );
    }
}
