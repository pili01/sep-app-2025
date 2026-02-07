package com.example.repository;

import com.example.model.AddressIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressIndexRepository extends JpaRepository<AddressIndex, Long> {
}
