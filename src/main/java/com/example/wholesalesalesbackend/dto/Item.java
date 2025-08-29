package com.example.wholesalesalesbackend.dto;

import lombok.Data;

@Data
public class Item {
    public String description;
    public int qty;
    public double mrp;
    public double amount;
}