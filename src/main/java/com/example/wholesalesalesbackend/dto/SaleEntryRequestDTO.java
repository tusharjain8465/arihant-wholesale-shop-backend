package com.example.wholesalesalesbackend.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SaleEntryRequestDTO {
    private String accessoryName;
    private Integer quantity;
    private Double totalPrice;
    private Double profit;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime saleDateTime;

    private Boolean returnFlag;
    private Long clientId;
    private String note;
}
