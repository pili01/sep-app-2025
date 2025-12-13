package com.example.WebShopSEP.service;

import com.example.WebShopSEP.dto.insurance.InsuranceCreateDTO;
import com.example.WebShopSEP.dto.insurance.InsuranceResponseDTO;
import com.example.WebShopSEP.dto.insurance.InsuranceUpdateDTO;
import com.example.WebShopSEP.model.Insurance;
import com.example.WebShopSEP.repository.InsuranceRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class InsuranceService {

    private final InsuranceRepository insuranceRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public InsuranceService(InsuranceRepository insuranceRepository, ModelMapper modelMapper) {
        this.insuranceRepository = insuranceRepository;
        this.modelMapper = modelMapper;
    }

    public List<InsuranceResponseDTO> findAll() {
        return insuranceRepository.findAll().stream()
                .map(insurance -> modelMapper.map(insurance, InsuranceResponseDTO.class))
                .collect(Collectors.toList());
    }

    public InsuranceResponseDTO findById(Integer id) {
        Insurance insurance = insuranceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Insurance not found with id: " + id));
        return modelMapper.map(insurance, InsuranceResponseDTO.class);
    }

    public InsuranceResponseDTO save(InsuranceCreateDTO insuranceCreateDTO) {
        Insurance insurance = modelMapper.map(insuranceCreateDTO, Insurance.class);
        Insurance savedInsurance = insuranceRepository.save(insurance);
        return modelMapper.map(savedInsurance, InsuranceResponseDTO.class);
    }

    public InsuranceResponseDTO update(Integer id, InsuranceUpdateDTO insuranceUpdateDTO) {
        Insurance insurance = insuranceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Insurance not found with id: " + id));

        modelMapper.map(insuranceUpdateDTO, insurance);
        Insurance updatedInsurance = insuranceRepository.save(insurance);
        return modelMapper.map(updatedInsurance, InsuranceResponseDTO.class);
    }

    public void deleteById(Integer id) {
        if (!insuranceRepository.existsById(id)) {
            throw new RuntimeException("Insurance not found with id: " + id);
        }
        insuranceRepository.deleteById(id);
    }
}

