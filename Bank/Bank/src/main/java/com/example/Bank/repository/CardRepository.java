package com.example.Bank.repository;

import com.example.Bank.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    Optional<Card> findByCardNumberAndDeletedFalse(String cardNumber);
    
    Optional<Card> findByCardNumberAndCvvAndCardholderNameAndExpirationDateAndDeletedFalse(
        String cardNumber, 
        String cvv, 
        String cardholderName, 
        String expirationDate
    );

    List<Card> findAllByDeletedFalse();

    List<Card> findAllByAccountUserEmailAndDeletedFalse(String email);

    Optional<Card> findByIdAndDeletedFalse(Long id);

    boolean existsByCardNumberAndDeletedFalse(String cardNumber);

    List<Card> findAllByAccountUserIdAndDeletedFalse(Long userId);
}

