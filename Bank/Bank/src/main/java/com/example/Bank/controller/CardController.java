package com.example.Bank.controller;

import com.example.Bank.dto.card.CreateCardRequest;
import com.example.Bank.jwt.JwtService;
import com.example.Bank.service.CardService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
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

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createCard(@RequestBody CreateCardRequest request) {
        try {
            return ResponseEntity.ok(cardService.createCard(request));
        } catch (RuntimeException e) {

            if (e.getMessage().equals("Account not found")) {
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
        return ResponseEntity.ok(cardService.getAllCards());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCard(@PathVariable Long id) {
        try {
            cardService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    @GetMapping("/user-cards")
    public ResponseEntity<?> getMyCards(HttpServletRequest request) {

        String token = JwtService.extractTokenFromRequest(request);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = jwtService.getUserIdFromToken(token);

        return ResponseEntity.ok(
                cardService.getMyCards(userId)
        );
    }
}