package com.example.wholesalesalesbackend.controllers;

import java.io.ByteArrayOutputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.example.wholesalesalesbackend.dto.GraphResponseDTO;
import com.example.wholesalesalesbackend.dto.ProfitAndSaleAndDeposit;
import com.example.wholesalesalesbackend.dto.SaleAttributeUpdateDTO;
import com.example.wholesalesalesbackend.dto.SaleEntryDTO;
import com.example.wholesalesalesbackend.dto.SaleEntryRequestDTO;
import com.example.wholesalesalesbackend.dto.SaleUpdateRequest;
import com.example.wholesalesalesbackend.model.Expense;
import com.example.wholesalesalesbackend.model.InBill;
import com.example.wholesalesalesbackend.model.SaleEntry;
import com.example.wholesalesalesbackend.model.User;
import com.example.wholesalesalesbackend.repository.ExpenseRepository;
import com.example.wholesalesalesbackend.repository.InBillRepository;
import com.example.wholesalesalesbackend.repository.SaleEntryRepository;
import com.example.wholesalesalesbackend.repository.UserClientRepository;
import com.example.wholesalesalesbackend.repository.UserRepository;
import com.example.wholesalesalesbackend.service.SaleEntryService;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/sales")
public class SaleEntryController {

    @Autowired(required = false)
    private SaleEntryService saleEntryService;

    @Autowired(required = false)
    private UserRepository userRepository;

    @Autowired(required = false)
    SaleEntryRepository saleEntryRepository;

    @Autowired(required = false)
    UserClientRepository userClientRepository;

    @Autowired(required = false)
    ExpenseRepository expenseRepository;

    @Autowired(required = false)
    InBillRepository inBillRepository;

    @PostMapping("/sale-entry/add")
    public ResponseEntity<String> addSaleEntry(@RequestBody SaleEntryRequestDTO requestDTO, @RequestParam Long userId) {
        saleEntryService.addSaleEntry(requestDTO, userId);
        return ResponseEntity.ok("added");
    }

    @PostMapping("/sale-entry/add-return")
    public ResponseEntity<String> addReturnEntry(@RequestBody SaleEntryRequestDTO requestDTO,
            @RequestParam Long userId) {

        String accessoryName = requestDTO.getAccessoryName();
        if (accessoryName != null && accessoryName.startsWith("ADD ->")) {
            accessoryName = accessoryName.replace("ADD ->", "").trim();
        }

        requestDTO.setReturnFlag(true);
        requestDTO.setAccessoryName(accessoryName);
        requestDTO.setSaleDateTime(null); // since it’s a return, don’t keep original date

        saleEntryService.addSaleEntry(requestDTO, userId);
        return ResponseEntity.ok("Return entry added successfully");
    }

    @GetMapping("/all-sales/all")
    public ResponseEntity<List<SaleEntryDTO>> getAllSales(@RequestParam Long userId) {
        List<SaleEntry> entries = saleEntryService.getAllSales(userId);

        List<SaleEntryDTO> dtos = new ArrayList<>();
        for (SaleEntry sale : entries) {

            SaleEntryDTO dto = new SaleEntryDTO();
            dto.setId(sale.getId());
            dto.setProfit(sale.getProfit());
            dto.setQuantity(sale.getQuantity());
            dto.setClientName(sale.getClient().getName());
            dto.setSaleDateTime(sale.getSaleDateTime());
            dto.setTotalPrice(sale.getTotalPrice());
            dto.setReturnFlag(sale.getReturnFlag());
            dto.setNote(sale.getNote());
            dto.setAccessoryName(sale.getAccessoryName());

            Optional<User> user = userRepository.findById(sale.getUserId());
            if (user.isPresent()) {
                dto.setAddedBy(user.get().getUsername());
            } else {
                dto.setAddedBy("unknown");
            }

            dtos.add(dto);

        }

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/all-sales-new")
    public ResponseEntity<Page<SaleEntryDTO>> getAllSales(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = true) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "search", required = false) String searchText) {

        // ✅ Map entity field -> DB column
        Sort sort = Sort.by(Sort.Order.desc("sale_date_time")); // DB column name
        Pageable pageable = PageRequest.of(page, size, sort);

        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(23, 59, 59) : null;

        Page<SaleEntry> entries;
        if (clientId == null) {
            entries = saleEntryRepository.findAllWithFiltersWithUserId(
                    startDateTime, endDateTime, searchText, userId, pageable);
        } else {
            entries = saleEntryRepository.findAllWithFiltersWithClientId(
                    clientId, startDateTime, endDateTime, searchText, pageable);
        }

        Page<SaleEntryDTO> dtos = entries.map(this::toDTO);
        return ResponseEntity.ok(dtos);
    }

    private SaleEntryDTO toDTO(SaleEntry entry) {
        SaleEntryDTO dto = new SaleEntryDTO();
        dto.setId(entry.getId());
        dto.setAccessoryName(entry.getAccessoryName());
        dto.setQuantity(entry.getQuantity());
        dto.setTotalPrice(entry.getTotalPrice());
        dto.setProfit(entry.getProfit());
        dto.setReturnFlag(entry.getReturnFlag());
        dto.setSaleDateTime(entry.getSaleDateTime());

        Optional<User> user = userRepository.findById(entry.getUserId());
        if (user.isPresent()) {
            dto.setAddedBy(user.get().getUsername());
        } else {
            dto.setAddedBy("unknown");
        }

        if (entry.getClient() != null) {
            dto.setClientName(entry.getClient().getName());
        }

        return dto;
    }

    @GetMapping("/by-date-range")
    public ResponseEntity<List<SaleEntry>> getSalesByDateRange(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime from,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime to) {
        return ResponseEntity.ok(saleEntryService.getSalesByDateRange(from, to, userId));
    }

    @GetMapping("/by-client/{clientId}")
    public ResponseEntity<List<SaleEntryDTO>> getSalesByClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(saleEntryService.getSalesEntryDTOByClient(clientId));
    }

    @PutMapping("/by-client/{clientId}")
    public ResponseEntity<String> updateSalesByClient(
            @PathVariable Long clientId, @RequestParam(value = "saleEntryId", required = true) Long saleEntryId,
            @RequestBody SaleUpdateRequest request) {
        saleEntryService.updateSalesByClient(clientId, saleEntryId, request);
        return ResponseEntity.ok("Updated !!!");
    }

    @GetMapping("/by-client-and-date-range")
    public ResponseEntity<List<SaleEntryDTO>> getSalesByClientAndDateRange(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = true) Long userId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime to) {

        List<SaleEntryDTO> sales = saleEntryService.getSalesEntryDTOByClientAndDateRange(clientId, from, to, userId);
        return ResponseEntity.ok(sales);
    }

    @PutMapping("/sale-entry/few-attributes")
    public ResponseEntity<String> updateProfit(@RequestBody SaleAttributeUpdateDTO dto) {
        saleEntryService.updateProfit(dto);
        return ResponseEntity.ok("updated!!!");
    }

    @PutMapping("/edit/{id}")
    public ResponseEntity<SaleEntryDTO> updateSaleEntry(@PathVariable Long id,
            @RequestBody @Valid SaleEntryDTO requestDTO) {
        SaleEntryDTO updated = saleEntryService.updateSaleEntry(id, requestDTO);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteSaleEntry(@PathVariable Long id) {
        String output = saleEntryService.deleteSaleEntry(id);
        return ResponseEntity.ok(output);
    }

    @DeleteMapping("/softdelete/{id}")
    public ResponseEntity<String> deleteSoftSaleEntry(@PathVariable Long id) {
        String output = saleEntryService.deleteSoftSaleEntry(id);
        return ResponseEntity.ok(output);
    }

    @DeleteMapping("/restore/{id}")
    public ResponseEntity<String> restoreSaleEntry(@PathVariable Long id) {
        String output = saleEntryService.restoreSaleEntry(id);
        return ResponseEntity.ok(output);
    }

    @GetMapping("/profit/by-date-range")
    public ResponseEntity<ProfitAndSaleAndDeposit> getProfitByDateRange(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime to,
            @RequestParam(required = false) Long days,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long clientId) {

        return ResponseEntity.ok(saleEntryService.getTotalProfitByDateRange(from, to, days, clientId, userId));
    }

    public List<SaleEntry> filterNonDeleted(List<SaleEntry> entries) {
        return entries.stream()
                .filter(entry -> Boolean.FALSE.equals(entry.getDeleteFlag()))
                .collect(Collectors.toList());
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> downloadSalesPdf(
            @RequestParam String period,
            @RequestParam Long userId,
            @RequestParam(required = false) Long clientId) throws Exception {

        // Fetch sales data
        GraphResponseDTO data = getSalesData(period, userId, clientId);

        byte[] pdfBytes = generateSalesPdf(
                period,
                data.getLabels(),
                data.getSalesData(),
                data.getProfitData(),
                data.getExpensesData(),
                data.getOutBillData());

        // Prepare filename
        java.time.LocalDate now = java.time.LocalDate.now();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd_MMMM_yyyy");
        String generationDate = now.format(formatter);

        // Determine month of report
        String reportMonth = "";
        if (period.equalsIgnoreCase("today") || period.equalsIgnoreCase("week")) {
            reportMonth = now.getMonth().name(); // e.g., AUGUST
        } else if (period.equalsIgnoreCase("month")) {
            reportMonth = data.getLabels().get(0); // first month label
        }

        String filename = period.toLowerCase() + "_" + reportMonth + "_" + generationDate + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    public byte[] generateSalesPdf(String period, List<String> labels, List<Double> salesData,
            List<Double> profitData, List<Double> expensesData, List<Double> outBillData) throws Exception {

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();

        // ---------------- Heading ----------------
        Font headingFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);

        String periodLabel = period.toUpperCase();
        String monthYearLabel = "";

        if (period.equalsIgnoreCase("today") || period.equalsIgnoreCase("week")) {
            // Get current month and year
            java.time.LocalDate now = java.time.LocalDate.now();
            monthYearLabel = now.getMonth().name() + " " + now.getYear();
        } else if (period.equalsIgnoreCase("month")) {
            // If monthly report, assume labels contain month names, take first month
            monthYearLabel = labels.get(0) + " " + java.time.LocalDate.now().getYear();
        }

        Paragraph heading = new Paragraph("Sales Report - " + periodLabel + " (" + monthYearLabel + ")", headingFont);
        heading.setAlignment(Element.ALIGN_CENTER);
        heading.setSpacingAfter(20f);
        document.add(heading);

        // Date of generation
        Font dateFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);
        Paragraph date = new Paragraph("Date of Generation: " + java.time.LocalDate.now(), dateFont);
        date.setAlignment(Element.ALIGN_CENTER);
        date.setSpacingAfter(15f);
        document.add(date);

        // ---------------- Table ----------------
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);
        table.setWidths(new float[] { 2f, 2f, 2f, 2f, 2f });

        // Fonts
        Font tableHeaderFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font tableCellFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);

        // Table headers
        String[] headers = { "Time/Day", "Sale (₹)", "Profit (₹)", "Expenses (₹)", "Out Bill (₹)" };
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, tableHeaderFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table.addCell(cell);
        }

        // Table rows
        for (int i = 0; i < labels.size(); i++) {
            PdfPCell cellLabel = new PdfPCell(new Phrase(labels.get(i), tableCellFont));
            cellLabel.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cellLabel);

            PdfPCell cellSale = new PdfPCell(new Phrase(String.valueOf(Math.round(salesData.get(i))), tableCellFont));
            cellSale.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cellSale);

            PdfPCell cellProfit = new PdfPCell(
                    new Phrase(String.valueOf(Math.round(profitData.get(i))), tableCellFont));
            cellProfit.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cellProfit);

            PdfPCell cellExpense = new PdfPCell(
                    new Phrase(String.valueOf(Math.round(expensesData.get(i))), tableCellFont));
            cellExpense.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cellExpense);

            PdfPCell cellOutBill = new PdfPCell(
                    new Phrase(String.valueOf(Math.round(outBillData.get(i))), tableCellFont));
            cellOutBill.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cellOutBill);
        }

        document.add(table);

        // ---------------- Totals Section ----------------
        PdfPTable totalsTable = new PdfPTable(2); // label + value
        totalsTable.setWidthPercentage(50);
        totalsTable.setSpacingBefore(15f);
        totalsTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        totalsTable.setWidths(new float[] { 2f, 2f });

        Font labelFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font valueFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);
        Font finalFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BaseColor.BLUE);

        double totalSale = salesData.stream().mapToDouble(Double::doubleValue).sum();
        double totalProfit = profitData.stream().mapToDouble(Double::doubleValue).sum();
        double totalExpenses = expensesData.stream().mapToDouble(Double::doubleValue).sum();
        double totalOutBill = outBillData.stream().mapToDouble(Double::doubleValue).sum();
        double finalAmount = totalSale - totalExpenses - totalOutBill;

        BiConsumer<String, Double> addRow = (label, value) -> {
            PdfPCell cell1 = new PdfPCell(new Phrase(label, labelFont));
            cell1.setBorder(Rectangle.NO_BORDER);
            cell1.setHorizontalAlignment(Element.ALIGN_LEFT);
            totalsTable.addCell(cell1);

            PdfPCell cell2 = new PdfPCell(new Phrase(String.valueOf(Math.round(value)), valueFont));
            cell2.setBorder(Rectangle.NO_BORDER);
            cell2.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalsTable.addCell(cell2);
        };

        addRow.accept("Total Sale", totalSale);
        addRow.accept("Total Profit", totalProfit);
        addRow.accept("Total Expenses", totalExpenses);
        addRow.accept("Total Out Bill", totalOutBill);

        // Final amount
        PdfPCell finalLabel = new PdfPCell(new Phrase("Final Amount", labelFont));
        finalLabel.setBorder(Rectangle.NO_BORDER);
        finalLabel.setHorizontalAlignment(Element.ALIGN_LEFT);
        totalsTable.addCell(finalLabel);

        PdfPCell finalValue = new PdfPCell(new Phrase(String.valueOf(Math.round(finalAmount)), finalFont));
        finalValue.setBorder(Rectangle.NO_BORDER);
        finalValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalsTable.addCell(finalValue);

        document.add(totalsTable);

        document.close();
        return baos.toByteArray();
    }

    @GetMapping("/graph-data")
    public GraphResponseDTO getSalesData(@RequestParam String period,
            @RequestParam Long userId,
            @RequestParam(required = false) Long clientId) {

        GraphResponseDTO response = new GraphResponseDTO();
        LocalDate today = LocalDate.now();

        List<String> labels = new ArrayList<>();
        List<Double> salesData = new ArrayList<>();
        List<Double> profitData = new ArrayList<>();
        List<Double> expensesData = new ArrayList<>();
        List<Double> outBillData = new ArrayList<>();

        // Determine client IDs
        List<Long> clientIds = clientId == null
                ? userClientRepository.fetchClientIdsByUserId(userId)
                : Collections.singletonList(clientId);

        // Determine date range and labels
        LocalDateTime startDate, endDate;
        switch (period.toLowerCase()) {
            case "today": {
                YearMonth currentMonth = YearMonth.from(today);
                startDate = currentMonth.atDay(1).atStartOfDay();
                endDate = currentMonth.atEndOfMonth().atTime(23, 59, 59);

                labels = IntStream.rangeClosed(1, today.lengthOfMonth())
                        .mapToObj(String::valueOf)
                        .collect(Collectors.toList());
                break;
            }
            case "week": {
                LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
                startDate = startOfWeek.atStartOfDay();
                endDate = startOfWeek.plusDays(6).atTime(23, 59, 59);

                labels = Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
                break;
            }
            case "month": {
                startDate = LocalDate.of(today.getYear(), 1, 1).atStartOfDay();
                endDate = LocalDate.of(today.getYear(), 12, 31).atTime(23, 59, 59);

                labels = Arrays.asList("Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");
                break;
            }
            default:
                throw new IllegalArgumentException("Invalid period. Use today, week, or month.");
        }

        // Fetch data
        List<SaleEntry> salesEntries = filterNonDeleted(
                saleEntryRepository.findAllByClient_IdInAndDeleteFlagFalseAndSaleDateTimeBetween(clientIds, startDate,
                        endDate));
        List<Expense> expenses = expenseRepository.findByClientIdInAndDatetimeISTBetween(clientIds, startDate, endDate);
        List<InBill> outBills = inBillRepository.findByClientIdInAndDateBetweenAndIsInBillFalse(clientIds, startDate,
                endDate);

        // Aggregate data
        switch (period.toLowerCase()) {
            case "today":
            case "week": {
                LocalDate startLabelDate = period.equalsIgnoreCase("week")
                        ? today.with(DayOfWeek.MONDAY)
                        : LocalDate.of(today.getYear(), today.getMonth(), 1);

                int days = labels.size();
                for (int i = 0; i < days; i++) {
                    LocalDate date = startLabelDate.plusDays(i);

                    Double dailySale = salesEntries.stream()
                            .filter(e -> e.getSaleDateTime().toLocalDate().equals(date))
                            .mapToDouble(e -> Optional.ofNullable(e.getTotalPrice()).orElse(0.0))
                            .sum();

                    Double dailyProfit = salesEntries.stream()
                            .filter(e -> e.getSaleDateTime().toLocalDate().equals(date))
                            .mapToDouble(e -> Optional.ofNullable(e.getProfit()).orElse(0.0))
                            .sum();

                    Double dailyExpense = expenses.stream()
                            .filter(e -> e.getDatetimeIST().toLocalDate().equals(date))
                            .mapToDouble(e -> Optional.ofNullable(e.getAmount()).orElse(0.0))
                            .sum();

                    Double dailyOutBill = outBills.stream()
                            .filter(b -> b.getDate().toLocalDate().equals(date))
                            .mapToDouble(b -> Optional.ofNullable(b.getAmount()).orElse(0.0))
                            .sum();

                    salesData.add(dailySale);
                    profitData.add(dailyProfit);
                    expensesData.add(dailyExpense);
                    outBillData.add(dailyOutBill);
                }
                break;
            }
            case "month": {
                for (int month = 1; month <= 12; month++) {
                    final int currentMonth = month; // make final for lambda

                    Double monthlySale = salesEntries.stream()
                            .filter(e -> e.getSaleDateTime().getMonthValue() == currentMonth)
                            .mapToDouble(e -> Optional.ofNullable(e.getTotalPrice()).orElse(0.0))
                            .sum();

                    Double monthlyProfit = salesEntries.stream()
                            .filter(e -> e.getSaleDateTime().getMonthValue() == currentMonth)
                            .mapToDouble(e -> Optional.ofNullable(e.getProfit()).orElse(0.0))
                            .sum();

                    Double monthlyExpense = expenses.stream()
                            .filter(e -> e.getDatetimeIST().getMonthValue() == currentMonth)
                            .mapToDouble(e -> Optional.ofNullable(e.getAmount()).orElse(0.0))
                            .sum();

                    Double monthlyOutBill = outBills.stream()
                            .filter(b -> b.getDate().getMonthValue() == currentMonth)
                            .mapToDouble(b -> Optional.ofNullable(b.getAmount()).orElse(0.0))
                            .sum();

                    salesData.add(monthlySale);
                    profitData.add(monthlyProfit);
                    expensesData.add(monthlyExpense);
                    outBillData.add(monthlyOutBill);
                }
                break;
            }
        }

        // Set response
        response.setLabels(labels);
        response.setSalesData(salesData);
        response.setProfitData(profitData);
        response.setExpensesData(expensesData);
        response.setOutBillData(outBillData);

        // Calculate totals
        double totalExpense = expensesData.stream().mapToDouble(Double::doubleValue).sum();
        double totalOutBill = outBillData.stream().mapToDouble(Double::doubleValue).sum();
        response.setTotalExpense(Math.round(totalExpense * 100.0) / 100.0);
        response.setTotalOutBill(Math.round(totalOutBill * 100.0) / 100.0);

        // Other stats
        response.setAverageSale(
                Math.round(salesData.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100.0) / 100.0);
        response.setAverageProfit(
                Math.round(profitData.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100.0) / 100.0);
        response.setHighestSale(
                Math.round(salesData.stream().mapToDouble(Double::doubleValue).max().orElse(0) * 100.0) / 100.0);
        response.setHighestProfit(
                Math.round(profitData.stream().mapToDouble(Double::doubleValue).max().orElse(0) * 100.0) / 100.0);

        return response;
    }

    @GetMapping("/all-sales-deleted")
    public ResponseEntity<List<SaleEntryDTO>> getAllSalesDeleted(@RequestParam Long userId) {

        List<SaleEntryDTO> entries = saleEntryService.findAllDeleted(userId);

        return ResponseEntity.ok(entries);
    }

    @GetMapping("/all-sales-deleted/by-client/{clientId}")
    public ResponseEntity<List<SaleEntryDTO>> getAllByClientSalesDeleted(@PathVariable Long clientId,
            @RequestParam Long userId) {

        List<SaleEntryDTO> entries = saleEntryService.findAllByClientDeleted(clientId);

        return ResponseEntity.ok(entries);
    }

    @GetMapping("/trash/count")
    public ResponseEntity<Long> getCountOfTrash(
            @RequestParam Long userId) {

        Long count = saleEntryService.getCountOfTrash(userId);

        return ResponseEntity.ok(count);
    }

    @GetMapping("/history/count")
    public ResponseEntity<Long> getCountOfHistory(
            @RequestParam Long userId) {

        Long count = saleEntryService.getCountOfHistory(userId);

        return ResponseEntity.ok(count);
    }

    @GetMapping("/deposit/count")
    public ResponseEntity<Long> getCountOfDeposit(
            @RequestParam Long userId) {

        Long count = saleEntryService.getCountOfDeposit(userId);

        return ResponseEntity.ok(count);
    }

    public List<SaleEntry> filterDeleted(List<SaleEntry> entries) {
        return entries.stream()
                .filter(entry -> Boolean.TRUE.equals(entry.getDeleteFlag()))
                .collect(Collectors.toList());
    }

}
