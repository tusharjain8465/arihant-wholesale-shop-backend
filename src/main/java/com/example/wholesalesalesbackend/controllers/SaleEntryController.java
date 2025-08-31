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

import org.postgresql.translation.messages_bg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
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
import com.example.wholesalesalesbackend.model.Deposit;
import com.example.wholesalesalesbackend.model.Expense;
import com.example.wholesalesalesbackend.model.InBill;
import com.example.wholesalesalesbackend.model.SaleEntry;
import com.example.wholesalesalesbackend.model.User;
import com.example.wholesalesalesbackend.repository.DepositRepository;
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

    @Autowired(required = false)
    DepositRepository depositRepository;

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
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long clientId) throws Exception {

        // Fetch sales data
        GraphResponseDTO data = getSalesData(userId, clientId, period, month, year);

        byte[] pdfBytes = generateSalesPdf(period,
                data.getLabels(),
                data.getSalesData(),
                data.getProfitData(),
                data.getExpensesData(),
                data.getOutBillData(),
                data.getInBillData(),
                data.getDepositData(), month, year);

        // Prepare filename
        java.time.LocalDate now = java.time.LocalDate.now();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd_MMMM_yyyy");
        String generationDate = now.format(formatter);

        String reportMonth = "";
        if (period.equalsIgnoreCase("today") || period.equalsIgnoreCase("week")) {
            reportMonth = now.getMonth().name();
        } else if (period.equalsIgnoreCase("month")) {
            reportMonth = data.getLabels().get(0);
        }

        String filename = period.toLowerCase() + "_" + reportMonth + "_" + generationDate + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    public byte[] generateSalesPdf(String period, List<String> labels, List<Double> salesData,
            List<Double> profitData, List<Double> expensesData, List<Double> outBillData, List<Double> inBillData,
            List<Double> depositData, Integer month, Integer year) throws Exception {

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();

        // ---------------- Heading ----------------
        Font headingFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
        String monthYearLabel;

        if (month != null && year != null) {
            // Convert month number to full month name
            java.time.Month monthEnum = java.time.Month.of(month); // 1 = January
            monthYearLabel = monthEnum.name().substring(0, 1).toUpperCase()
                    + monthEnum.name().substring(1).toLowerCase()
                    + " " + year;
        } else {
            java.time.LocalDate now = java.time.LocalDate.now();
            java.time.Month monthEnum = now.getMonth();
            monthYearLabel = monthEnum.name().substring(0, 1).toUpperCase()
                    + monthEnum.name().substring(1).toLowerCase()
                    + " " + now.getYear();
        }

        Paragraph heading = new Paragraph("Sales Report - " + monthYearLabel, headingFont);
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
        PdfPTable table = new PdfPTable(7); // 7 columns
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);
        table.setWidths(new float[] { 2f, 2f, 2f, 2f, 2f, 2f, 2f });

        Font tableHeaderFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.WHITE);
        Font tableCellFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);

        // Table headers in the required sequence
        BaseColor headerColor = new BaseColor(0, 121, 182); // Blue
        String[] headers = { "Time/Day", "Sale (₹)", "Profit (₹)", "Deposit (₹)", "In Bill (₹)", "Out Bill (₹)",
                "Expenses (₹)" };
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, tableHeaderFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(headerColor);
            table.addCell(cell);
        }

        // Table rows with alternating colors
        BaseColor rowColor1 = new BaseColor(224, 235, 255); // Light Blue
        BaseColor rowColor2 = BaseColor.WHITE;

        for (int i = 0; i < labels.size(); i++) {
            BaseColor bgColor = (i % 2 == 0) ? rowColor1 : rowColor2;

            table.addCell(createCell(labels.get(i), tableCellFont, bgColor));
            table.addCell(createCell(Math.round(salesData.get(i)), tableCellFont, bgColor));
            table.addCell(createCell(Math.round(profitData.get(i)), tableCellFont, bgColor));
            table.addCell(createCell(Math.round(depositData.get(i)), tableCellFont, bgColor));
            table.addCell(createCell(Math.round(inBillData.get(i)), tableCellFont, bgColor));
            table.addCell(createCell(-Math.round(Math.abs(outBillData.get(i))), tableCellFont, bgColor));
            table.addCell(createCell(Math.round(expensesData.get(i)), tableCellFont, bgColor));
        }

        // ---------------- Grand Total Row ----------------
        Font totalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.WHITE);
        BaseColor totalColor = new BaseColor(0, 82, 163); // Dark Blue

        Double totalSale = salesData.stream().mapToDouble(Double::doubleValue).sum();
        Double totalProfit = profitData.stream().mapToDouble(Double::doubleValue).sum();
        Double totalDeposit = depositData.stream().mapToDouble(Double::doubleValue).sum();
        Double totalInBill = inBillData.stream().mapToDouble(Double::doubleValue).sum();
        Double totalOutBill = outBillData.stream().mapToDouble(Double::doubleValue).sum();
        Double totalExpenses = expensesData.stream().mapToDouble(Double::doubleValue).sum();

        table.addCell(createCell("GRAND TOTAL", totalFont, totalColor));
        table.addCell(createCell(Math.round(totalSale), totalFont, totalColor));
        table.addCell(createCell(Math.round(totalProfit), totalFont, totalColor));
        table.addCell(createCell(Math.round(totalDeposit), totalFont, totalColor));
        table.addCell(createCell(Math.round(totalInBill), totalFont, totalColor));
        table.addCell(createCell(-Math.round(Math.abs(totalOutBill)), totalFont, totalColor));
        table.addCell(createCell(Math.round(totalExpenses), totalFont, totalColor));

        document.add(table);

        // ---------------- Remaining Balance ----------------
        PdfPTable balanceTable = new PdfPTable(2);
        balanceTable.setWidthPercentage(50);
        balanceTable.setSpacingBefore(10f);
        balanceTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        balanceTable.setWidths(new float[] { 3f, 2f });

        Font balanceFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BaseColor.WHITE);
        BaseColor balanceColor = new BaseColor(220, 50, 50); // Red

        PdfPCell balanceLabel = new PdfPCell(new Phrase("Remaining Balance (Sale - Out Bill - Expenses)", balanceFont));
        balanceLabel.setBorder(Rectangle.NO_BORDER);
        balanceLabel.setBackgroundColor(balanceColor);
        balanceLabel.setHorizontalAlignment(Element.ALIGN_CENTER);
        balanceTable.addCell(balanceLabel);

        Double remainingBalance = totalSale - Math.abs(totalOutBill) - totalExpenses;
        PdfPCell balanceValue = new PdfPCell(new Phrase(String.valueOf(Math.round(remainingBalance)), balanceFont));
        balanceValue.setBorder(Rectangle.NO_BORDER);
        balanceValue.setBackgroundColor(balanceColor);
        balanceValue.setHorizontalAlignment(Element.ALIGN_CENTER);
        balanceTable.addCell(balanceValue);

        document.add(balanceTable);

        document.close();
        return baos.toByteArray();
    }

    // Helper method to create table cells
    private PdfPCell createCell(Object value, Font font, BaseColor bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(String.valueOf(value), font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(bgColor);
        return cell;
    }

    @GetMapping("/graph-data")
    public GraphResponseDTO getSalesData(
            @RequestParam Long userId,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) String period, // today, week, month, year
            @RequestParam(required = false) Integer month, // 1-12
            @RequestParam(required = false) Integer year // e.g. 2024
    ) {
        GraphResponseDTO response = new GraphResponseDTO();
        LocalDate today = LocalDate.now();

        List<Long> clientIds = clientId == null
                ? userClientRepository.fetchClientIdsByUserId(userId)
                : Collections.singletonList(clientId);

        List<String> labels = new ArrayList<>();
        List<Double> salesData = new ArrayList<>();
        List<Double> profitData = new ArrayList<>();
        List<Double> expensesData = new ArrayList<>();
        List<Double> inBillData = new ArrayList<>();
        List<Double> outBillData = new ArrayList<>();
        List<Double> depositData = new ArrayList<>();

        LocalDateTime startDate;
        LocalDateTime endDate;

        // ---------------- MONTH & YEAR FILTER ----------------
        if (month != null && year != null) {
            YearMonth yearMonth = YearMonth.of(year, month);
            startDate = yearMonth.atDay(1).atStartOfDay();
            endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);

            labels = IntStream.rangeClosed(1, yearMonth.lengthOfMonth())
                    .mapToObj(String::valueOf)
                    .collect(Collectors.toList());

            // Fetch data
            List<SaleEntry> salesEntries = saleEntryRepository
                    .findAllByClient_IdInAndDeleteFlagFalseAndSaleDateTimeBetween(clientIds, startDate, endDate);

            List<Expense> expenses = expenseRepository
                    .findByClientIdInAndDatetimeISTBetween(clientIds, startDate, endDate);

            List<InBill> outBills = inBillRepository
                    .findByClientIdInAndDateBetweenAndIsInBillFalse(clientIds, startDate, endDate);

            List<InBill> inBills = inBillRepository
                    .findByClientIdInAndDateBetweenAndIsInBillTrue(clientIds, startDate, endDate);

            List<Deposit> deposits = depositRepository
                    .findByClientIdInAndDepositDateBetween(clientIds, startDate, endDate);

            for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
                LocalDate date = LocalDate.of(year, month, day);

                double dailySale = salesEntries.stream()
                        .filter(e -> e.getSaleDateTime().toLocalDate().equals(date))
                        .mapToDouble(e -> Optional.ofNullable(e.getTotalPrice()).orElse(0.0))
                        .sum();

                double dailyProfit = salesEntries.stream()
                        .filter(e -> e.getSaleDateTime().toLocalDate().equals(date))
                        .mapToDouble(e -> Optional.ofNullable(e.getProfit()).orElse(0.0))
                        .sum();

                double dailyExpense = expenses.stream()
                        .filter(e -> e.getDatetimeIST().toLocalDate().equals(date))
                        .mapToDouble(e -> Optional.ofNullable(e.getAmount()).orElse(0.0))
                        .sum();

                double dailyOutBill = outBills.stream()
                        .filter(b -> b.getDate().toLocalDate().equals(date))
                        .mapToDouble(b -> Optional.ofNullable(b.getAmount()).orElse(0.0))
                        .sum();

                double dailyInBill = inBills.stream()
                        .filter(b -> b.getDate().toLocalDate().equals(date))
                        .mapToDouble(b -> Optional.ofNullable(b.getAmount()).orElse(0.0))
                        .sum();

                double dailyDeposit = deposits.stream()
                        .filter(b -> b.getDepositDate().toLocalDate().equals(date))
                        .mapToDouble(b -> Optional.ofNullable(b.getAmount()).orElse(0.0))
                        .sum();

                salesData.add(dailySale);
                profitData.add(dailyProfit);
                expensesData.add(dailyExpense);
                outBillData.add(dailyOutBill);
                inBillData.add(dailyInBill);
                depositData.add(dailyDeposit);
            }
        }
        // ---------------- PERIOD FILTER ----------------
        else if (period != null) {
            switch (period.toLowerCase()) {
                case "today": {
                    startDate = today.atStartOfDay();
                    endDate = today.atTime(23, 59, 59);
                    labels = Collections.singletonList(today.toString());
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
                    YearMonth currentMonth = YearMonth.from(today);
                    startDate = currentMonth.atDay(1).atStartOfDay();
                    endDate = currentMonth.atEndOfMonth().atTime(23, 59, 59);
                    labels = IntStream.rangeClosed(1, currentMonth.lengthOfMonth())
                            .mapToObj(String::valueOf)
                            .collect(Collectors.toList());
                    break;
                }
                case "year": {
                    startDate = LocalDate.of(today.getYear(), 1, 1).atStartOfDay();
                    endDate = LocalDate.of(today.getYear(), 12, 31).atTime(23, 59, 59);
                    labels = Arrays.asList("Jan", "Feb", "Mar", "Apr", "May", "Jun",
                            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");
                    break;
                }
                default:
                    throw new IllegalArgumentException("Invalid period. Use today, week, month, or year.");
            }

            // Fetch data
            List<SaleEntry> salesEntries = saleEntryRepository
                    .findAllByClient_IdInAndDeleteFlagFalseAndSaleDateTimeBetween(clientIds, startDate, endDate);

            List<Expense> expenses = expenseRepository
                    .findByClientIdInAndDatetimeISTBetween(clientIds, startDate, endDate);

            List<InBill> outBills = inBillRepository
                    .findByClientIdInAndDateBetweenAndIsInBillFalse(clientIds, startDate, endDate);

            List<InBill> inBills = inBillRepository
                    .findByClientIdInAndDateBetweenAndIsInBillTrue(clientIds, startDate, endDate);

            List<Deposit> deposits = depositRepository
                    .findByClientIdInAndDepositDateBetween(clientIds, startDate, endDate);

            if (period.equalsIgnoreCase("today")) {
                // Single day totals
                double totalSale = salesEntries.stream()
                        .mapToDouble(e -> Optional.ofNullable(e.getTotalPrice()).orElse(0.0)).sum();
                double totalProfit = salesEntries.stream()
                        .mapToDouble(e -> Optional.ofNullable(e.getProfit()).orElse(0.0)).sum();
                double totalExpense = expenses.stream().mapToDouble(e -> Optional.ofNullable(e.getAmount()).orElse(0.0))
                        .sum();
                double totalOutBill = outBills.stream().mapToDouble(b -> Optional.ofNullable(b.getAmount()).orElse(0.0))
                        .sum();
                double totalInBill = inBills.stream().mapToDouble(b -> Optional.ofNullable(b.getAmount()).orElse(0.0))
                        .sum();
                double totalDeposit = deposits.stream().mapToDouble(d -> Optional.ofNullable(d.getAmount()).orElse(0.0))
                        .sum();

                salesData.add(totalSale);
                profitData.add(totalProfit);
                expensesData.add(totalExpense);
                outBillData.add(totalOutBill);
                inBillData.add(totalInBill);
                depositData.add(totalDeposit);
            } else if (period.equalsIgnoreCase("week")) {
                for (int i = 0; i < 7; i++) {
                    LocalDate date = today.with(DayOfWeek.MONDAY).plusDays(i);

                    double dailySale = salesEntries.stream()
                            .filter(e -> e.getSaleDateTime().toLocalDate().equals(date))
                            .mapToDouble(e -> Optional.ofNullable(e.getTotalPrice()).orElse(0.0))
                            .sum();

                    double dailyProfit = salesEntries.stream()
                            .filter(e -> e.getSaleDateTime().toLocalDate().equals(date))
                            .mapToDouble(e -> Optional.ofNullable(e.getProfit()).orElse(0.0))
                            .sum();

                    double dailyExpense = expenses.stream()
                            .filter(e -> e.getDatetimeIST().toLocalDate().equals(date))
                            .mapToDouble(e -> Optional.ofNullable(e.getAmount()).orElse(0.0))
                            .sum();

                    double dailyOutBill = outBills.stream()
                            .filter(b -> b.getDate().toLocalDate().equals(date))
                            .mapToDouble(b -> Optional.ofNullable(b.getAmount()).orElse(0.0))
                            .sum();

                    double dailyInBill = inBills.stream()
                            .filter(b -> b.getDate().toLocalDate().equals(date))
                            .mapToDouble(b -> Optional.ofNullable(b.getAmount()).orElse(0.0))
                            .sum();

                    double dailyDeposit = deposits.stream()
                            .filter(b -> b.getDepositDate().toLocalDate().equals(date))
                            .mapToDouble(b -> Optional.ofNullable(b.getAmount()).orElse(0.0))
                            .sum();

                    salesData.add(dailySale);
                    profitData.add(dailyProfit);
                    expensesData.add(dailyExpense);
                    outBillData.add(dailyOutBill);
                    inBillData.add(dailyInBill);
                    depositData.add(dailyDeposit);
                }
            } else if (period.equalsIgnoreCase("month")) {
                YearMonth currentMonth = YearMonth.from(today);
                for (int day = 1; day <= currentMonth.lengthOfMonth(); day++) {
                    LocalDate date = LocalDate.of(today.getYear(), today.getMonth(), day);

                    double dailySale = salesEntries.stream()
                            .filter(e -> e.getSaleDateTime().toLocalDate().equals(date))
                            .mapToDouble(e -> Optional.ofNullable(e.getTotalPrice()).orElse(0.0))
                            .sum();

                    double dailyProfit = salesEntries.stream()
                            .filter(e -> e.getSaleDateTime().toLocalDate().equals(date))
                            .mapToDouble(e -> Optional.ofNullable(e.getProfit()).orElse(0.0))
                            .sum();

                    double dailyExpense = expenses.stream()
                            .filter(e -> e.getDatetimeIST().toLocalDate().equals(date))
                            .mapToDouble(e -> Optional.ofNullable(e.getAmount()).orElse(0.0))
                            .sum();

                    double dailyOutBill = outBills.stream()
                            .filter(b -> b.getDate().toLocalDate().equals(date))
                            .mapToDouble(b -> Optional.ofNullable(b.getAmount()).orElse(0.0))
                            .sum();

                    double dailyInBill = inBills.stream()
                            .filter(b -> b.getDate().toLocalDate().equals(date))
                            .mapToDouble(b -> Optional.ofNullable(b.getAmount()).orElse(0.0))
                            .sum();

                    double dailyDeposit = deposits.stream()
                            .filter(b -> b.getDepositDate().toLocalDate().equals(date))
                            .mapToDouble(b -> Optional.ofNullable(b.getAmount()).orElse(0.0))
                            .sum();

                    salesData.add(dailySale);
                    profitData.add(dailyProfit);
                    expensesData.add(dailyExpense);
                    outBillData.add(dailyOutBill);
                    inBillData.add(dailyInBill);
                    depositData.add(dailyDeposit);
                }
            } else if (period.equalsIgnoreCase("year")) {
                for (int m = 1; m <= 12; m++) {
                    YearMonth ym = YearMonth.of(today.getYear(), m);

                    double monthlySale = salesEntries.stream()
                            .filter(e -> YearMonth.from(e.getSaleDateTime().toLocalDate()).equals(ym))
                            .mapToDouble(e -> Optional.ofNullable(e.getTotalPrice()).orElse(0.0))
                            .sum();

                    double monthlyProfit = salesEntries.stream()
                            .filter(e -> YearMonth.from(e.getSaleDateTime().toLocalDate()).equals(ym))
                            .mapToDouble(e -> Optional.ofNullable(e.getProfit()).orElse(0.0))
                            .sum();

                    double monthlyExpense = expenses.stream()
                            .filter(e -> YearMonth.from(e.getDatetimeIST().toLocalDate()).equals(ym))
                            .mapToDouble(e -> Optional.ofNullable(e.getAmount()).orElse(0.0))
                            .sum();

                    double monthlyOutBill = outBills.stream()
                            .filter(b -> YearMonth.from(b.getDate().toLocalDate()).equals(ym))
                            .mapToDouble(b -> Optional.ofNullable(b.getAmount()).orElse(0.0))
                            .sum();

                    double monthlyInBill = inBills.stream()
                            .filter(b -> YearMonth.from(b.getDate().toLocalDate()).equals(ym))
                            .mapToDouble(b -> Optional.ofNullable(b.getAmount()).orElse(0.0))
                            .sum();

                    double monthlyDeposit = deposits.stream()
                            .filter(b -> YearMonth.from(b.getDepositDate().toLocalDate()).equals(ym))
                            .mapToDouble(b -> Optional.ofNullable(b.getAmount()).orElse(0.0))
                            .sum();

                    salesData.add(monthlySale);
                    profitData.add(monthlyProfit);
                    expensesData.add(monthlyExpense);
                    outBillData.add(monthlyOutBill);
                    inBillData.add(monthlyInBill);
                    depositData.add(monthlyDeposit);
                }
            }
        }

        // ---------------- RESPONSE ----------------
        response.setLabels(labels);
        response.setSalesData(salesData);
        response.setProfitData(profitData);
        response.setExpensesData(expensesData);
        response.setOutBillData(outBillData);
        response.setInBillData(inBillData);
        response.setDepositData(depositData);

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

