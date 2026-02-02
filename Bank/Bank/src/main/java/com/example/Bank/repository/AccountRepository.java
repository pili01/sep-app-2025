package com.example.Bank.repository;

import com.example.Bank.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByMerchantId(String merchantId);

    Optional<Account> findByAccountNumberHashAndDeletedFalse(String accountNumberHash);

    Optional<Account> findByMerchantIdAndDeletedFalse(String merchantId);

    @Query("SELECT a FROM Account a JOIN a.user u WHERE u.email = :email AND a.deleted = false")
    Optional<Account> findByUserEmailAndDeletedFalse(String email);

    List<Account> findAllByDeletedFalse();

    Optional<Account> findByIdAndDeletedFalse(Long id);

    Optional<Account> findByUserId(Long userId);
}

