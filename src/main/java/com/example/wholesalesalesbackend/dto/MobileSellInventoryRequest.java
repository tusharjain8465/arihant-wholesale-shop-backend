package com.example.wholesalesalesbackend.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MobileSellInventoryRequest {

    public LocalDate soldDate;
    public double price;
    public String soldTo;

}
