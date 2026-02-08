package com.example.WebShopSEP.repository;

import com.example.WebShopSEP.model.Rental;
import com.example.WebShopSEP.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RentalRepository extends JpaRepository<Rental,Long> {

    @Query("""
            SELECT DISTINCT r FROM Rental r
                join fetch r.equipment
                    join fetch r.user
                      join fetch r.vehicle
                      left join fetch r.insurance
                    WHERE r.user.id = :userId
            """)
    List<Rental> findByUser(@Param("userId") Long userId);
}
