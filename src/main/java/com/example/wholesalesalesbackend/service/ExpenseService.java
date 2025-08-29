package com.example.wholesalesalesbackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.wholesalesalesbackend.model.Expense;
import com.example.wholesalesalesbackend.repository.ExpenseRepository;
import com.example.wholesalesalesbackend.repository.UserClientRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserClientRepository userClientRepository;

    public List<Expense> getAllExpenses(Long userId) {
        List<Long> clientIds = userClientRepository.fetchClientIdsByUserId(userId);
        return expenseRepository.findByClientIdIn(clientIds);
    }

    public Optional<Expense> getExpenseById(Long id) {
        return expenseRepository.findById(id);
    }

    public Expense createExpense(Expense expense) {
        return expenseRepository.save(expense);
    }

    public Expense updateExpense(Long id, Expense updatedExpense) {
        Optional<Expense> existOpt = expenseRepository.findById(id);

        if (existOpt.isPresent()) {
            Expense exist = existOpt.get();

            // Use updated datetime or current IST datetime
            LocalDateTime updatedDateTime = updatedExpense.getDatetimeIST();
            if (updatedDateTime == null) {
                updatedDateTime = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
            }

            // Update fields
            exist.setAmount(updatedExpense.getAmount());
            exist.setNote(updatedExpense.getNote());
            exist.setType(updatedExpense.getType());
            exist.setDatetimeIST(updatedDateTime);
            exist.setClientId(updatedExpense.getClientId());

            // Save and return
            return expenseRepository.save(exist);
        } else {
            throw new RuntimeException("Expense not found with id " + id);
        }
    }

    public void deleteExpense(Long id) {
        expenseRepository.deleteById(id);
    }

    public List<Expense> getExpensesByDateRange(Long userId, Long clientId, LocalDateTime from, LocalDateTime to) {
        if (clientId == null) {
            List<Long> clientIds = userClientRepository.fetchClientIdsByUserId(userId);
            return expenseRepository.findByClientIdInAndDatetimeISTBetween(clientIds, from, to);
        } else {
            return expenseRepository.findByClientIdAndDatetimeISTBetween(clientId, from, to);
        }
    }

}
