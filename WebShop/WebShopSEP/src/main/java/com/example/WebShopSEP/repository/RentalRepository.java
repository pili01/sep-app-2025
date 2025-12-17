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

    @Query("SELECT DISTINCT r FROM Rental r " +
           "LEFT JOIN FETCH r.user " +
           "LEFT JOIN FETCH r.vehicle " +
           "LEFT JOIN FETCH r.insurance " +
           "LEFT JOIN FETCH r.equipment " +
           "WHERE r.user = :user")
    List<Rental> findByUser(User user);

    @Query("SELECT DISTINCT r FROM Rental r " +
           "LEFT JOIN FETCH r.user " +
           "LEFT JOIN FETCH r.vehicle " +
           "LEFT JOIN FETCH r.insurance " +
           "LEFT JOIN FETCH r.equipment")
    List<Rental> findAllForAuthor();
}
