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
public class SaleEntryDTO {

    private Long id;

    private String accessoryName;

    private Integer quantity;

    private Double totalPrice;

    private Double profit;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Kolkata")
    private LocalDateTime saleDateTime;

    private Boolean returnFlag;

    private String clientName;

    private String note;

    private String addedBy;
}
