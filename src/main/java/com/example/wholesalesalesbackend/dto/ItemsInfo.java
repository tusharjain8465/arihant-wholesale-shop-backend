package com.example.wholesalesalesbackend.dto;

import lombok.Data;

@Data
public class ItemsInfo {
    private String description;
    private int qty;
    private double rate;     // per item rate
    private double gstPercent; // e.g., 18
    private double amount;   // auto = qty * rate
}