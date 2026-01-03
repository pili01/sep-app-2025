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

    private final CryptoService cryptoService;

    public MerchantService(MerchantRepository merchantRepository, CryptoService cryptoService) {
        this.merchantRepository = merchantRepository;
        this.cryptoService = cryptoService;
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
            savedMerchant.setMerchantPassword(cryptoService.encrypt(UUID.randomUUID().toString()));

            return merchantRepository.save(savedMerchant);

        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException(
                    "Merchant with name '" + dto.getName() + "' already exists."
            );
        }
    }

    public List<Merchant> getAll() {
        List<Merchant> merchants = merchantRepository.findAll();

        for(Merchant m: merchants){
            m.setMerchantPassword(cryptoService.decrypt(m.getMerchantPassword()));
        }

        return merchants;
    }

    public Merchant getById(Long id) {
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Merchant with id " + id + " not found."
                ));

        merchant.setMerchantPassword(cryptoService.decrypt(merchant.getMerchantPassword()));

        return merchant;
    }

    public boolean verifyCredentials(String id, String inputPassword) {
        return merchantRepository.findByMerchantId(id)
                .map(merchant -> {
                    try {
                        String decryptedStoredPassword = cryptoService.decrypt(merchant.getMerchantPassword());

                        return decryptedStoredPassword.equals(inputPassword);
                    } catch (Exception e) {
                        System.err.println("Decryption failed for merchant: " + id);
                        return false;
                    }
                })
                .orElse(false);
    }
}