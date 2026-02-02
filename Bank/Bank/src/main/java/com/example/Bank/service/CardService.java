package com.example.Bank.service;

import com.example.Bank.dto.card.CardResponse;
import com.example.Bank.dto.card.CreateCardRequest;
import com.example.Bank.model.Account;
import com.example.Bank.model.Card;
import com.example.Bank.repository.AccountRepository;
import com.example.Bank.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;
    private final ModelMapper modelMapper;
    private final CryptoService cryptoService;

    /* ================= ADMIN ================= */

    public CardResponse createCard(CreateCardRequest request) {

        Account account = accountRepository.findByIdAndDeletedFalse(request.getAccountId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        String pan = request.getCardNumber().replaceAll("\\D", "");
        String panHash = cryptoService.hashDeterministic(pan);
        String panEnc = cryptoService.encrypt(pan);
        String cvvEnc = cryptoService.encrypt(request.getCvv());

        if (!validateLuhn(pan)) {
            throw new RuntimeException("Invalid card number");
        }

        if (!validateExpirationDate(request.getExpirationDate())) {
            throw new RuntimeException("Invalid expiration date");
        }

        if (cardRepository.existsByPanHashAndDeletedFalse(panHash)) {
            throw new RuntimeException("Card already exists");
        }

        Card card = new Card();
        card.setPanEnc(panEnc);
        card.setPanHash(panHash);
        card.setCardholderName(account.getAccountHolderName());
        card.setExpirationDate(request.getExpirationDate());
        card.setCvvEnc(cvvEnc);
        card.setAccount(account);
        card.setDeleted(false);

        return mapToResponse(cardRepository.save(card));
    }

    public List<CardResponse> getAllCards() {
        return cardRepository.findAllByDeletedFalse()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public void delete(Long cardId) {
        Card card = cardRepository.findByIdAndDeletedFalse(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        card.setDeleted(true);
        cardRepository.save(card);
    }

    /* ================= USER ================= */

    public List<CardResponse> getMyCards(Long userId) {
        return cardRepository
                .findAllByAccountUserIdAndDeletedFalse(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /* ================= HELPERS ================= */

    private CardResponse mapToResponse(Card card) {
        return modelMapper.map(card, CardResponse.class);
    }

    public boolean validateLuhn(String pan) {
        if (pan == null) {
            return false;
        }

        String digits = pan.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return false;
        }

        int sum = 0;
        boolean doubleDigit = false;

        for (int i = digits.length() - 1; i >= 0; i--) {
            int digit = digits.charAt(i) - '0';

            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            doubleDigit = !doubleDigit;
        }

        return sum % 10 == 0;
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


