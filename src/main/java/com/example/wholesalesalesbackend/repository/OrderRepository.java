package com.example.wholesalesalesbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.wholesalesalesbackend.model.Order;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findAllByOrderByDateTimeDesc();

    List<Order> findAllByUserIdAndClientIdOrderByDateTimeDesc(Long userId, Long clientId);

    List<Order> findAllByUserIdAndClientIdAndSupplierOrderByDateTimeDesc(Long userId, Long clientId, String supplier);

}
