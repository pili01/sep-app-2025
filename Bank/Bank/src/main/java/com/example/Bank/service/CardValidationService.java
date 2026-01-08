package com.example.Bank.service;

import com.example.Bank.model.Card;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class CardValidationService {
    

    public boolean validateLuhn(String pan) {
        if (pan == null || pan.isEmpty()) {
            return false;
        }

        String digits = pan.replaceAll("\\D", "");
        Card tempCard = new Card();
        tempCard.setCardNumber(digits);
        return tempCard.isValid();
    }
    

    public boolean validateExpirationDate(String expirationDate) {
        if (expirationDate == null || !expirationDate.matches("\\d{2}/\\d{2}")) {
            return false;
        }
        
        try {
            String[] parts = expirationDate.split("/");
            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt(parts[1]);

            if (month < 1 || month > 12) {
                return false;
            }

            LocalDate now = LocalDate.now();
            int currentYear = now.getYear() % 100;
            int currentMonth = now.getMonthValue();
            
            if (year < currentYear) {
                return false;
            }
            
            if (year == currentYear && month < currentMonth) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    public String detectCardType(String pan) {
        if (pan == null || pan.isEmpty()) {
            return null;
        }

        String digits = pan.replaceAll("\\D", "");
        
        if (digits.startsWith("4")) {
            return "VISA";
        } else if (digits.startsWith("5") || digits.startsWith("2")) {
            return "MASTERCARD";
        }
        
        return null;
    }
}





