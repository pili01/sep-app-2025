package com.example.WebShopSEP.controller;

import com.example.WebShopSEP.dto.rental.RentalDto;
import com.example.WebShopSEP.service.RentalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/rental")
public class RentalController {
    public final RentalService rentalService;

    public RentalController(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/")
    public ResponseEntity<Object> createRental(@Valid @RequestBody RentalDto rentalDto) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            String response = rentalService.createRental(rentalDto, email);
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getRentalById(@PathVariable Long id) {
        try {
            RentalDto rentalDto = rentalService.getRentalById(id);
            return ResponseEntity.ok().body(rentalDto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/my")
    public ResponseEntity<Object> getAllMyRentals() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            return ResponseEntity.ok().body(rentalService.getAllMyRentals(email));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('AUTHOR')")
    @GetMapping("/all")
    public ResponseEntity<Object> getAllRentals() {
        try {
            return ResponseEntity.ok().body(rentalService.getAllRentals());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> cancelRental(@PathVariable Long id) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            String response = rentalService.cancelRental(id, email);
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PatchMapping("/{id}/pay")
    public ResponseEntity<Object> payForRental(@PathVariable Long id) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            String response = rentalService.payForRental(id, email);
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
