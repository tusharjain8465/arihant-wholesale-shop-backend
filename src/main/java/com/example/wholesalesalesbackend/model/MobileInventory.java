package com.example.wholesalesalesbackend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "mobile_inventory")
public class MobileInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mobile_name")
    private String mobileName;

    @Column(nullable = false)
    private String imei1;

    @Column(nullable = false)
    private String imei2;

    @Column(name = "supplier_name")
    private String supplierName;

    @Column(name = "sold_to")
    private String soldTo;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "sold_date")
    private LocalDate soldDate;

    @Column(nullable = false)
    private Double price;

    @Column(name = "created_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime createdAt = LocalDateTime.now();

    // getters & setters
}
