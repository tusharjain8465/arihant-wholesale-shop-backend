package com.example.wholesalesalesbackend.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MobileInventoryRequest {

    public String mobileName;
    public String imei1;
    public String imei2;
    public String supplierName;
    public String soldTo;
    public LocalDate purchaseDate;
    public LocalDate soldDate;
    public Double price;
}
