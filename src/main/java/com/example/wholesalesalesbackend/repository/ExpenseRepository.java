package com.example.wholesalesalesbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.wholesalesalesbackend.model.Expense;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // Find expenses between two dates
    List<Expense> findByDatetimeISTBetween(LocalDateTime from, LocalDateTime to);

    List<Expense> findByClientIdIn(List<Long> clientIds);

    List<Expense> findByClientIdInAndDatetimeISTBetween(List<Long> clientIds, LocalDateTime from, LocalDateTime to);

    List<Expense> findByClientIdAndDatetimeISTBetween(Long clientId, LocalDateTime from, LocalDateTime to);

   }