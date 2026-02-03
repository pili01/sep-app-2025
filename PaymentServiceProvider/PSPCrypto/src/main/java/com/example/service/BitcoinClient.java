package com.example.service;

import com.example.dto.CryptoConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BitcoinClient {

    private final ObjectMapper objectMapper;

    /**
     * Parsira crypto config iz JSON stringa
     */
    public CryptoConfig parseConfig(String configJson) {
        try {
            return objectMapper.readValue(configJson, CryptoConfig.class);
        } catch (Exception e) {
            log.error("Error parsing crypto config", e);
            throw new RuntimeException("Invalid crypto configuration: " + e.getMessage(), e);
        }
    }

    // TODO: Implementirati metode za:
    // - convertToBitcoin() - konverzija fiat → BTC
    // - generateBitcoinAddress() - generisanje Bitcoin adrese
    // - checkPaymentStatus() - provera blockchain-a
}
