package com.example.wholesalesalesbackend.controllers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.wholesalesalesbackend.dto.BillDTO;
import com.example.wholesalesalesbackend.model.InBill;
import com.example.wholesalesalesbackend.repository.InBillRepository;
import com.example.wholesalesalesbackend.service.InBillService;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;

@RestController
@RequestMapping("/api/in-bills")
public class InBillController {

    @Autowired(required = false)
    private InBillService inBillService;

    @Autowired(required = false)
    private InBillRepository inBillRepository;

    @PostMapping("/in-bill")
    public InBill addInBill(
            @RequestParam(required = true) String supplier,
            @RequestParam(required = true) Double amount,
            @RequestParam(required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date,
            @RequestParam(required = false) MultipartFile[] files,
            @RequestParam(required = true) Long userId,
            @RequestParam(required = true) Long clientId) throws IOException {
        return inBillService.saveInBill(supplier, amount, date, files, userId, clientId);
    }

    @PostMapping("/out-bill")
    public InBill addOutBill(
            @RequestParam(required = true) String supplier,
            @RequestParam(required = true) Double amount,
            @RequestParam(required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date,
            @RequestParam(required = false) MultipartFile[] files,
            @RequestParam(required = true) Long userId,
            @RequestParam(required = true) Long clientId) throws IOException {
        return inBillService.saveOutBill(supplier, amount, date, files, userId, clientId);
    }

    @PutMapping("/amount-edit/{id}")
    public ResponseEntity<String> update(
            @PathVariable Long id, @RequestParam(value = "amount", required = true) Double amount) {
        inBillService.updateAmount(id, amount);
        return ResponseEntity.ok("Updated !!!");
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id) {
        inBillService.deleteBill(id);
        return ResponseEntity.ok("Deleted !!!");
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> downloadSupplierInOutPdf(
            @RequestParam String supplier,
            @RequestParam Long userId,
            @RequestParam(required = false) Long clientId) throws Exception {

        // Fetch IN and OUT bills
        List<InBill> allInBills = inBillRepository.findBySupplierAndClientIdAndUserIdAndIsInBillTrue(
                supplier, clientId, userId);
        List<InBill> allOutBills = inBillRepository.findBySupplierAndClientIdAndUserIdAndIsInBillFalse(
                supplier, clientId, userId);

        // Merge all bills with type info
        class BillWithType {
            InBill bill;
            String type;

            BillWithType(InBill bill, String type) {
                this.bill = bill;
                this.type = type;
            }
        }

        List<BillWithType> mergedBills = new ArrayList<>();
        allInBills.forEach(b -> mergedBills.add(new BillWithType(b, "IN")));
        allOutBills.forEach(b -> mergedBills.add(new BillWithType(b, "OUT")));

        // Sort by date ascending
        mergedBills.sort(Comparator.comparing(b -> b.bill.getDate()));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        document.add(new Paragraph("Supplier: " + supplier)
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                .setFontSize(16)
                .setMarginBottom(20));

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // --- SINGLE TABLE ---
        float[] columnWidths = { 50F, 150F, 100F, 100F };
        Table table = new Table(columnWidths);

        // Header row
        table.addHeaderCell(new Cell().add(new Paragraph("Sr No")).setBackgroundColor(ColorConstants.DARK_GRAY)
                .setFontColor(ColorConstants.WHITE));
        table.addHeaderCell(new Cell().add(new Paragraph("Date")).setBackgroundColor(ColorConstants.DARK_GRAY)
                .setFontColor(ColorConstants.WHITE));
        table.addHeaderCell(new Cell().add(new Paragraph("Amount")).setBackgroundColor(ColorConstants.DARK_GRAY)
                .setFontColor(ColorConstants.WHITE));
        table.addHeaderCell(new Cell().add(new Paragraph("Type (IN/OUT)")).setBackgroundColor(ColorConstants.DARK_GRAY)
                .setFontColor(ColorConstants.WHITE));

        int srNo = 1;
        double totalAmount = 0;

        // Add bills in sorted order
        for (BillWithType bwt : mergedBills) {
            double amount = bwt.bill.getAmount();
            if (bwt.type.equals("OUT")) {
                amount = -amount; // make OUT bills negative
            }
            totalAmount += amount;

            Color rowColor = bwt.type.equals("IN") ? ColorConstants.CYAN : ColorConstants.PINK;

            table.addCell(new Cell().add(new Paragraph(String.valueOf(srNo++))).setBackgroundColor(rowColor));
            table.addCell(new Cell().add(new Paragraph(bwt.bill.getDate().format(dateFormatter)))
                    .setBackgroundColor(rowColor));
            table.addCell(new Cell().add(new Paragraph(String.valueOf(amount))).setBackgroundColor(rowColor));
            table.addCell(new Cell().add(new Paragraph(bwt.type)).setBackgroundColor(rowColor));
        }

        // Total row
        Cell totalLabel = new Cell(1, 3).add(new Paragraph("Total Pending Amount"))
                .setBackgroundColor(ColorConstants.DARK_GRAY)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER);
        Cell totalValue = new Cell().add(new Paragraph(String.valueOf(totalAmount)))
                .setBackgroundColor(ColorConstants.DARK_GRAY)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.LEFT);

        table.addCell(totalLabel);
        table.addCell(totalValue);

        document.add(table);

        document.close();

        // Dynamic filename
        DateTimeFormatter fileDateFormatter = DateTimeFormatter.ofPattern("ddMMyyyy");
        String currentDate = LocalDate.now().format(fileDateFormatter);
        String fileName = supplier + "_" + currentDate + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", fileName);

        return ResponseEntity.ok()
                .headers(headers)
                .body(baos.toByteArray());
    }

    @GetMapping("/all-bills")
    public ResponseEntity<Page<BillDTO>> getAllBills(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String supplier,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "search", required = false) String searchText,
            @RequestParam(required = true) Long userId,
            @RequestParam(required = true) Long clientId) {

        Pageable pageable = PageRequest.of(page, size);

        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(23, 59, 59) : null;

        if (searchText != null) {
            searchText = searchText.toLowerCase();

            if (type != null) {
                searchText = searchText + " " + type.toLowerCase();
            }
        } else {

            if (type != null) {
                searchText = type.toLowerCase();
            }
        }

        String normalizedSearch = (searchText == null) ? "" : searchText.trim();

        Page<InBill> entries = inBillRepository.findAllWithFilters(supplier, startDateTime, endDateTime,
                normalizedSearch,
                userId, clientId, pageable);

        Page<BillDTO> pageDTOs = entries.map(this::toDTO);

        return ResponseEntity.ok(pageDTOs);
    }

    private BillDTO toDTO(InBill bill) {

        BillDTO billDTO = new BillDTO();
        billDTO.setId(bill.getId());
        billDTO.setSupplierName(bill.getSupplier());
        billDTO.setAmount(bill.getAmount());
        billDTO.setDateTime(bill.getDate());
        String type = "";
        if (bill.getIsInBill()) {
            type = "In Bill";
        } else {
            type = "Out Bill";
        }

        billDTO.setType(type);

        return billDTO;
    }
}
