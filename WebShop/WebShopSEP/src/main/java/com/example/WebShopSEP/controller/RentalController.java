package com.example.WebShopSEP.controller;

import com.example.WebShopSEP.dto.rental.RentalDto;
import com.example.WebShopSEP.service.RentalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;



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
        Logger logger = LoggerFactory.getLogger(this.getClass());

        // Debug 1: Authentication objekat
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            logger.warn("SecurityContextHolder.getContext().getAuthentication() je null!");
        } else {
            logger.info("Authentication objekt: {}", auth);
            logger.info("Korisničko ime (email): {}", auth.getName());
            logger.info("Authorities:");
            for (GrantedAuthority authority : auth.getAuthorities()) {
                logger.info(" - {}", authority.getAuthority());
            }
        }

        // Debug 2: rentalId
        logger.info("Pozvan payForRental sa rentalId: {}", id);

        try {
            String email = auth != null ? auth.getName() : "unknown";
            String response = rentalService.payForRental(id, email);

            // Debug 3: uspešan povratak iz servisa
            logger.info("Servis je uspešno izvršen, response: {}", response);

            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            logger.error("Greška u payForRental: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
