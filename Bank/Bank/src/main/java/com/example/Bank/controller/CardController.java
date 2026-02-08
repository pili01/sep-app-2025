package com.example.Bank.controller;

import com.example.Bank.dto.card.CreateCardRequest;
import com.example.Bank.jwt.JwtService;
import com.example.Bank.service.CardService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bank/cards")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CardController {

    private final CardService cardService;
    private final JwtService jwtService;

    private static final Logger log = LoggerFactory.getLogger(CardController.class);

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createCard(@RequestBody CreateCardRequest request) {
        log.info("Attempting to create card for accountId={}", request.getAccountId());
        try {
            var created = cardService.createCard(request);
            log.info("Card created successfully: cardId={}", created.getId());
            return ResponseEntity.ok(created);
        } catch (RuntimeException e) {
            log.error("Failed to create card for accountId={}: {}", request.getAccountId(), e.getMessage(), e);

            if ("Account not found".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(e.getMessage());
            }

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllCards() {
        log.info("Fetching all cards");
        var cards = cardService.getAllCards();
        log.info("Fetched {} cards", cards.size());
        return ResponseEntity.ok(cards);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCard(@PathVariable Long id) {
        log.info("Deleting card with id={}", id);
        try {
            cardService.delete(id);
            log.info("Card deleted successfully: id={}", id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Failed to delete card id={}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    @GetMapping("/user-cards")
    public ResponseEntity<?> getMyCards(HttpServletRequest request) {

        String token = JwtService.extractTokenFromRequest(request);
        if (token == null) {
            log.warn("Unauthorized attempt to fetch user cards");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = jwtService.getUserIdFromToken(token);
        log.info("Fetching cards for userId={}", userId);

        var cards = cardService.getMyCards(userId);
        log.info("Fetched {} cards for userId={}", cards.size(), userId);

        return ResponseEntity.ok(cards);
    }
}