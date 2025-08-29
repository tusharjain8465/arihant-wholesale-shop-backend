package com.example.wholesalesalesbackend.dto;

import java.util.List;
import lombok.Data;

@Data
public class QuotationRequest {
    private ShopInfo shop; // seller shop
    private CustomerInfo customer; // buyer
    private List<ItemsInfo> items;
    private String gstin;

    private double subTotal;
    private double gst;
    private double totalAmount;
    private String invoiceNo;
    private String invoiceDate;
    private String terms;

    // 🎨 New fields for color customization
    private String invoiceBgColor; // e.g. "#FFFFFF"
    private String customerBoxColor; // e.g. "#87CEEB" (SkyBlue)
    private String headerColor; // e.g. "#0079B6" (Blue for table header)
}