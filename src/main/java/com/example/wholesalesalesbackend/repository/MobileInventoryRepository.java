package com.example.wholesalesalesbackend.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.wholesalesalesbackend.model.MobileInventory;

public interface MobileInventoryRepository extends JpaRepository<MobileInventory, Long> {

    @Query("""
                SELECT m FROM MobileInventory m
                WHERE (:fromDate IS NULL OR m.purchaseDate >= :fromDate)
                AND (:toDate IS NULL OR m.purchaseDate <= :toDate)
            """)
    List<MobileInventory> filterByPurchaseDate(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
}
