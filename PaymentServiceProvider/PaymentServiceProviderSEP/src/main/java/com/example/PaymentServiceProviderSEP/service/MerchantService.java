package com.example.PaymentServiceProviderSEP.service;

import com.example.PaymentServiceProviderSEP.dto.merchant.CreateMerchantDTO;
import com.example.PaymentServiceProviderSEP.model.Merchant;
import com.example.PaymentServiceProviderSEP.model.MerchantStatus;
import com.example.PaymentServiceProviderSEP.repository.MerchantRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class MerchantService {

    private final MerchantRepository merchantRepository;

    public MerchantService(MerchantRepository merchantRepository) {
        this.merchantRepository = merchantRepository;
    }

    @Transactional
    public Merchant create(CreateMerchantDTO dto) {
        Merchant merchant = new Merchant();
        merchant.setName(dto.getName());
        merchant.setSuccessUrl(dto.getSuccessUrl());
        merchant.setFailedUrl(dto.getFailedUrl());
        merchant.setErrorUrl(dto.getErrorUrl());
        merchant.setStatus(MerchantStatus.DRAFT);

        try {
            Merchant savedMerchant = merchantRepository.save(merchant);

            savedMerchant.setMerchantId(UUID.randomUUID().toString());
            savedMerchant.setMerchantPassword(UUID.randomUUID().toString());

            return merchantRepository.save(savedMerchant);

        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException(
                    "Merchant with name '" + dto.getName() + "' already exists."
            );
        }
    }

    public List<Merchant> getAll() {
        return merchantRepository.findAll();
    }

    public Merchant getById(Long id) {
        return merchantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Merchant with id " + id + " not found."
                ));
    }

    public boolean verifyCredentials(String id, String password) {
        return merchantRepository.findByMerchantIdAndMerchantPassword(id, password).isPresent();
    }
}