package com.example.wholesalesalesbackend.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.example.wholesalesalesbackend.model.Expense;
import com.example.wholesalesalesbackend.service.ExpenseService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    // GET all expenses
    @GetMapping
    public List<Expense> getAllExpenses(@RequestParam Long userId) {
        return expenseService.getAllExpenses(userId);
    }

    // GET by ID
    @GetMapping("/{id}")
    public Expense getExpenseById(@PathVariable Long id) {
        return expenseService.getExpenseById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found with id " + id));
    }

    // POST create new expense
    @PostMapping
    public Expense createExpense(@RequestBody Expense expense) {
        return expenseService.createExpense(expense);
    }

    // PUT update expense
    @PutMapping("/{id}")
    public Expense updateExpense(@PathVariable Long id, @RequestBody Expense expense) {
        return expenseService.updateExpense(id, expense);
    }

    // DELETE expense
    @DeleteMapping("/{id}")
    public void deleteExpense(@PathVariable Long id) {
        expenseService.deleteExpense(id);
    }

    // GET expenses by date range
    @GetMapping("/filter")
    public List<Expense> getExpensesByDateRange(
            @RequestParam(required = false) Long clientId,
            @RequestParam Long userId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return expenseService.getExpensesByDateRange(userId,clientId, from, to);
    }
}