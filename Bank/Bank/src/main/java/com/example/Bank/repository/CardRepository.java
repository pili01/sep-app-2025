package com.example.Bank.repository;

import com.example.Bank.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    Optional<Card> findByPanHashAndDeletedFalse(String panHash);

    boolean existsByPanHashAndDeletedFalse(String panHash);

    List<Card> findAllByDeletedFalse();

    Optional<Card> findByIdAndDeletedFalse(Long id);

    List<Card> findAllByAccountUserIdAndDeletedFalse(Long userId);
}

