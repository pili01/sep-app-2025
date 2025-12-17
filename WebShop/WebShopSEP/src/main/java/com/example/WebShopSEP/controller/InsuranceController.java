package com.example.WebShopSEP.controller;

import com.example.WebShopSEP.dto.insurance.InsuranceCreateDTO;
import com.example.WebShopSEP.dto.insurance.InsuranceResponseDTO;
import com.example.WebShopSEP.dto.insurance.InsuranceUpdateDTO;
import com.example.WebShopSEP.service.InsuranceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/insurances")
public class InsuranceController {

    private final InsuranceService insuranceService;

    @Autowired
    public InsuranceController(InsuranceService insuranceService) {
        this.insuranceService = insuranceService;
    }

    @GetMapping
    public ResponseEntity<List<InsuranceResponseDTO>> getAllInsurances() {
        List<InsuranceResponseDTO> insurances = insuranceService.findAll();
        return ResponseEntity.ok(insurances);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InsuranceResponseDTO> getInsuranceById(@PathVariable Integer id) {
        try {
            InsuranceResponseDTO insurance = insuranceService.findById(id);
            return ResponseEntity.ok(insurance);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasRole('AUTHOR')")
    @PostMapping
    public ResponseEntity<InsuranceResponseDTO> createInsurance(@RequestBody InsuranceCreateDTO insuranceCreateDTO) {
        InsuranceResponseDTO createdInsurance = insuranceService.save(insuranceCreateDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdInsurance);
    }

    @PreAuthorize("hasRole('AUTHOR')")
    @PutMapping("/{id}")
    public ResponseEntity<InsuranceResponseDTO> updateInsurance(@PathVariable Integer id, @RequestBody InsuranceUpdateDTO insuranceUpdateDTO) {
        try {
            InsuranceResponseDTO updatedInsurance = insuranceService.update(id, insuranceUpdateDTO);
            return ResponseEntity.ok(updatedInsurance);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasRole('AUTHOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInsurance(@PathVariable Integer id) {
        try {
            insuranceService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

