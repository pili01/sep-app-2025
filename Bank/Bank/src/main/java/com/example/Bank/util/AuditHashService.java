package com.example.Bank.util;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class AuditHashService {

    private String lastHash = "GENESIS";

    public synchronized String calculateHash(String message) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = message + lastHash;
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            String hashHex = bytesToHex(hashBytes);
            lastHash = hashHex;
            return hashHex;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to calculate hash", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public String getLastHash() {
        return lastHash;
    }
}