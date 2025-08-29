package com.example.wholesalesalesbackend.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleAttributeUpdateDTO {
    private Long saleEntryId;
    private String Accessory;
    private Double totalPrice;
    private Double profit;
}
