
package com.example.wholesalesalesbackend.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleEntryUpdateDTO {

    private String accessoryName;

    private Double totalPrice;

    private Double profit;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd MMMM yyyy", timezone = "Asia/Kolkata")
    private LocalDateTime saleDateTime;

    private String clientName;
}
