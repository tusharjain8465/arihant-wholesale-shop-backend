package com.example.wholesalesalesbackend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.wholesalesalesbackend.model.Supplier;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    List<Supplier> findAllByClientId(Long clientId);
    // Optional: Add custom queries if needed

    List<Supplier> findAllByUserId(Long userId);

    List<Supplier> findAllByUserIdAndClientId(Long userId, Long clientId);
}
