package com.example.wholesalesalesbackend.dto;

import java.util.List;

import lombok.Data;

@Data
public class GraphResponseDTO {
  private List<String> labels;
  private List<Double> salesData;
  private List<Double> profitData;

  // ADD THESE FIELDS
  private List<Double> expensesData;
  private List<Double> outBillData;

  private double averageSale;
  private double averageProfit;
  private double highestSale;
  private double highestProfit;

  private double totalExpense;

  private double totalOutBill;

}
