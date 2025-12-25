package com.example.WebShopSEP.repository;

import com.example.WebShopSEP.model.Rental;
import com.example.WebShopSEP.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface RentalRepository extends JpaRepository<Rental,Integer> {

    @Query("SELECT r FROM Rental r WHERE r.user = :user")
    List<Rental> findByUser(User user);
}
