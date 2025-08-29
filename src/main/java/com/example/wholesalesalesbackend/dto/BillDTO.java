package com.example.wholesalesalesbackend.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class BillDTO {

    public Long id;
    public LocalDateTime dateTime;
    public String supplierName;
    public Double amount;
    public String type;
}
