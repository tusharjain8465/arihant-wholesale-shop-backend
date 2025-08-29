
package com.example.wholesalesalesbackend.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.example.wholesalesalesbackend.dto.Item;
import com.example.wholesalesalesbackend.dto.ItemsInfo;
import com.example.wholesalesalesbackend.dto.QuotationRequest;
import com.example.wholesalesalesbackend.model.Deposit;
import com.example.wholesalesalesbackend.model.SaleEntry;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

@Service
public class PdfService {

    static class UnifiedEntry {
        LocalDate date;
        String description;
        Double amount;
        boolean isReturn;
        boolean isDeposit;

        public UnifiedEntry(LocalDate date, String description, Double amount, boolean isReturn, boolean isDeposit) {
            this.date = date;
            this.description = description;
            this.amount = amount;
            this.isReturn = isReturn;
            this.isDeposit = isDeposit;
        }

        public LocalDate getDate() {
            return date;
        }
    }

    public ByteArrayInputStream generateSalesPdf(
            String clientName,
            List<SaleEntry> sales,
            List<Deposit> depositEntries,
            LocalDate from,
            LocalDate to,
            boolean isAllClient,
            Double depositAmount,
            java.time.LocalDateTime depositDateTime,
            Double oldBalance) {

        if (oldBalance == null)
            oldBalance = 0.0;

        ZoneId indiaZone = ZoneId.of("Asia/Kolkata"); // IST

        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        List<SaleEntry> filteredSales = filterNonDeleted(sales);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Font redFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.RED);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
            java.text.DecimalFormat noDecimalFormat = new java.text.DecimalFormat("#");

            // Title
            BaseColor pinkColor = new BaseColor(255, 105, 180);
            Font fontShopTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, pinkColor);

            Paragraph shopTitle = new Paragraph("Arihant Mobile Shop", fontShopTitle);
            shopTitle.setAlignment(Element.ALIGN_CENTER);
            shopTitle.setSpacingAfter(20f);
            document.add(shopTitle);

            // Report Info
            document.add(new Paragraph("Sales Report -> " + clientName + "", fontBold));
            if (from != null && to != null) {
                document.add(new Paragraph(
                        "" + from.format(formatter) + " To " + to.format(formatter) + " Report",
                        fontBold));
            }
            document.add(Chunk.NEWLINE);

            // Old Balance
            String oldBalPrefix = (from != null)
                    ? "(" + from.minusDays(1).format(formatter) + ") Pending Amount = ₹"
                    : "Pending Amount = ₹";
            Paragraph oldBalanceLine = new Paragraph(oldBalPrefix + noDecimalFormat.format(oldBalance), redFont);
            oldBalanceLine.setAlignment(Element.ALIGN_RIGHT);
            document.add(oldBalanceLine);
            document.add(Chunk.NEWLINE);

            // ===== Merge Sales & Deposits into unified list (no timezone conversion
            // needed) =====
            List<UnifiedEntry> unifiedList = new ArrayList<>();

            for (SaleEntry sale : filteredSales) {
                // DB already stores IST, so just extract LocalDate
                unifiedList.add(new UnifiedEntry(
                        sale.getSaleDateTime().toLocalDate(),
                        sale.getAccessoryName(),
                        sale.getTotalPrice(),
                        Boolean.TRUE.equals(sale.getReturnFlag()),
                        false));
            }

            for (Deposit dep : depositEntries) {
                // DB already stores IST, just extract LocalDate
                unifiedList.add(new UnifiedEntry(
                        dep.getDepositDate().toLocalDate(),
                        dep.getNote(),
                        -dep.getAmount(), // deposits negative
                        false,
                        true));
            }

            unifiedList.sort(Comparator.comparing(UnifiedEntry::getDate));

            // ===== Create unified table =====
            PdfPTable table = new PdfPTable(4); // Sr, Date, Description, Amount
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 10f, 25f, 45f, 20f });

            Stream.of("Sr", "Date", "Description", "Amount").forEach(header -> {
                PdfPCell cell = new PdfPCell(new Phrase(header, fontBold));
                cell.setBackgroundColor(new BaseColor(135, 206, 250)); // Light blue

                // Align Amount to right, others to left
                if ("Amount".equals(header)) {
                    cell.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
                } else {
                    cell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
                }

                table.addCell(cell);
            });

            double totalSales = 0.0;
            double totalDeposits = 0.0;
            int srNo = 1;

            for (UnifiedEntry entry : unifiedList) {
                BaseColor rowColor = null;
                if (entry.isDeposit)
                    rowColor = new BaseColor(255, 105, 180); // faint red
                if (entry.isReturn)
                    rowColor = new BaseColor(255, 255, 153); // faint yellow

                PdfPCell srCell = new PdfPCell(new Phrase(String.valueOf(srNo++), fontNormal));
                PdfPCell dateCell = new PdfPCell(new Phrase(entry.date.format(formatter), fontNormal));
                PdfPCell descCell = new PdfPCell(new Phrase(entry.description, fontNormal));
                PdfPCell amountCell = new PdfPCell(
                        new Phrase((entry.amount < 0 ? "-" : "") + "₹" + noDecimalFormat.format(Math.abs(entry.amount)),
                                fontNormal));
                amountCell.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);

                if (rowColor != null) {
                    srCell.setBackgroundColor(rowColor);
                    dateCell.setBackgroundColor(rowColor);
                    descCell.setBackgroundColor(rowColor);
                    amountCell.setBackgroundColor(rowColor);
                }

                table.addCell(srCell);
                table.addCell(dateCell);
                table.addCell(descCell);
                table.addCell(amountCell);

                if (!entry.isDeposit)
                    totalSales += entry.amount;
                else
                    totalDeposits += entry.amount;
            }

            document.add(table);
            document.add(Chunk.NEWLINE);

            // ===== Final Amount highlighted in red =====
            Double finalBalance = oldBalance + totalSales + totalDeposits;
            Font finalFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BaseColor.RED);

            Chunk finalChunk = new Chunk("[ " +
                    ZonedDateTime.now(indiaZone).format(formatter) +
                    " Final Amount = ₹" + noDecimalFormat.format(finalBalance) + " ]",
                    finalFont);
            finalChunk.setBackground(BaseColor.WHITE);

            Paragraph finalBalanceLine = new Paragraph(finalChunk);
            finalBalanceLine.setAlignment(Element.ALIGN_RIGHT);
            document.add(finalBalanceLine);

            document.add(Chunk.NEWLINE);
            document.add(Chunk.NEWLINE);

            // Footer
            Paragraph footer = new Paragraph(
                    "Thank You For Purchasing\nContact on Vishal Jain Mobile No : +91 9537886555",
                    fontBold);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(20f);
            document.add(footer);

            document.close();

        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF: " + e.getMessage());
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    public List<SaleEntry> filterNonDeleted(List<SaleEntry> entries) {
        return entries.stream()
                .filter(entry -> Boolean.FALSE.equals(entry.getDeleteFlag()))
                .collect(Collectors.toList());
    }

    // public void calculateTotals(QuotationRequest request) {
    // double subtotal = 0;
    // double totalGst = 0;

    // for (ItemsInfo item : request.getItems()) {
    // double itemAmount = item.getQty() * item.getRate();
    // item.setAmount(itemAmount);

    // double gstAmt = (itemAmount * item.getGstPercent()) / 100;
    // totalGst += gstAmt;

    // subtotal += itemAmount;
    // }

    // request.setSubTotal(subtotal);
    // request.setGst(totalGst);
    // request.setTotalAmount(subtotal + totalGst);
    // }

    // public ByteArrayInputStream generateInvoice(QuotationRequest request) {
    // calculateTotals(request);

    // Document document = new Document(PageSize.A4, 36, 36, 36, 36);
    // ByteArrayOutputStream out = new ByteArrayOutputStream();

    // try {
    // PdfWriter.getInstance(document, out);
    // document.open();

    // // Shop Info (Seller)
    // Font shopFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD,
    // BaseColor.BLACK);
    // document.add(new Paragraph(request.getShop().getName(), shopFont));
    // document.add(new Paragraph(request.getShop().getAddress()));
    // document.add(new Paragraph("Phone: " + request.getShop().getPhone()));
    // document.add(new Paragraph("GSTIN: " + request.getShop().getGstin()));
    // document.add(Chunk.NEWLINE);

    // // Invoice Info
    // document.add(new Paragraph("Invoice No: " + request.getInvoiceNo()));
    // document.add(new Paragraph("Date: " + request.getInvoiceDate()));
    // document.add(Chunk.NEWLINE);

    // // Customer Info (SkyBlue)
    // PdfPCell custCell = new PdfPCell();
    // custCell.addElement(
    // new Phrase("Customer", new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD,
    // BaseColor.BLACK)));
    // custCell.addElement(new Phrase(request.getCustomer().getName()));
    // custCell.addElement(new Phrase(request.getCustomer().getAddress()));
    // custCell.addElement(new Phrase("Phone: " +
    // request.getCustomer().getPhone()));
    // custCell.setBackgroundColor(new BaseColor(135, 206, 235)); // Sky Blue
    // custCell.setPadding(10);

    // PdfPTable custTable = new PdfPTable(1);
    // custTable.setWidthPercentage(100);
    // custTable.addCell(custCell);
    // document.add(custTable);
    // document.add(Chunk.NEWLINE);

    // // Items Table
    // PdfPTable itemTable = new PdfPTable(6);
    // itemTable.setWidthPercentage(100);
    // itemTable.setWidths(new int[] { 4, 1, 2, 2, 1, 2 });

    // Font headFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD,
    // BaseColor.WHITE);
    // addHeaderCell(itemTable, "Description", headFont);
    // addHeaderCell(itemTable, "Qty", headFont);
    // addHeaderCell(itemTable, "Rate", headFont);
    // addHeaderCell(itemTable, "Amount", headFont);
    // addHeaderCell(itemTable, "GST%", headFont);
    // addHeaderCell(itemTable, "GST Amt", headFont);

    // for (ItemsInfo item : request.getItems()) {
    // double gstAmt = (item.getAmount() * item.getGstPercent()) / 100;
    // itemTable.addCell(item.getDescription());
    // itemTable.addCell(String.valueOf(item.getQty()));
    // itemTable.addCell("₹ " + item.getRate());
    // itemTable.addCell("₹ " + item.getAmount());
    // itemTable.addCell(item.getGstPercent() + "%");
    // itemTable.addCell("₹ " + gstAmt);
    // }

    // document.add(itemTable);
    // document.add(Chunk.NEWLINE);

    // // Totals
    // document.add(new Paragraph("Subtotal: ₹ " + request.getSubTotal()));
    // document.add(new Paragraph("GST: ₹ " + request.getGst()));
    // document.add(new Paragraph("Total Amount: ₹ " + request.getTotalAmount(),
    // shopFont));

    // document.add(Chunk.NEWLINE);

    // // Terms
    // document.add(new Paragraph("Terms & Conditions:"));
    // document.add(new Paragraph(request.getTerms()));

    // document.add(Chunk.NEWLINE);
    // document.add(new Paragraph("Seal & Signature ____________________"));

    // document.close();
    // } catch (Exception e) {
    // e.printStackTrace();
    // }

    // return new ByteArrayInputStream(out.toByteArray());
    // }

    // private void addHeaderCell(PdfPTable table, String text, Font font) {
    // PdfPCell cell = new PdfPCell(new Phrase(text, font));
    // cell.setBackgroundColor(new BaseColor(0, 121, 182)); // Blue
    // cell.setHorizontalAlignment(Element.ALIGN_CENTER);
    // cell.setPadding(6);
    // table.addCell(cell);
    // }

    public void calculateTotals(QuotationRequest request) {
        double subtotal = 0;
        double totalGst = 0;

        for (ItemsInfo item : request.getItems()) {
            double itemAmount = item.getQty() * item.getRate();
            item.setAmount(itemAmount);

            double gstAmt = (itemAmount * item.getGstPercent()) / 100;
            totalGst += gstAmt;
            subtotal += itemAmount;
        }

        request.setSubTotal(subtotal);
        request.setGst(totalGst);
        request.setTotalAmount(subtotal + totalGst);
    }

    public ByteArrayInputStream generateInvoice(QuotationRequest request) {
        calculateTotals(request);

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // ✅ 1. Convert HEX codes from payload to BaseColor
            BaseColor invoiceBg = request.getInvoiceBgColor() != null ? hexToBaseColor(request.getInvoiceBgColor())
                    : BaseColor.WHITE;
            BaseColor customerBox = request.getCustomerBoxColor() != null
                    ? hexToBaseColor(request.getCustomerBoxColor())
                    : new BaseColor(173, 216, 230); // SkyBlue
            BaseColor headerColor = request.getHeaderColor() != null ? hexToBaseColor(request.getHeaderColor())
                    : new BaseColor(0, 121, 182);

            // 🔹 Shop Name (Big + Center)
            Font shopNameFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, BaseColor.BLACK);
            Paragraph shopName = new Paragraph(request.getShop().getName(), shopNameFont);
            shopName.setAlignment(Element.ALIGN_CENTER);
            document.add(shopName);

            // 🔹 Shop details (small under shop name)
            Font shopDetailFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.DARK_GRAY);
            Paragraph shopDetails = new Paragraph(
                    request.getShop().getAddress() + " | Phone: " + request.getShop().getPhone() +
                            " | GSTIN: " + request.getShop().getGstin(),
                    shopDetailFont);
            shopDetails.setAlignment(Element.ALIGN_CENTER);
            document.add(shopDetails);

            document.add(Chunk.NEWLINE);

            // 🔹 Invoice Info (Right aligned)
            PdfPTable invoiceInfoTable = new PdfPTable(1);
            invoiceInfoTable.setWidthPercentage(100);

            PdfPCell infoCell = new PdfPCell();
            infoCell.setBorder(Rectangle.NO_BORDER);
            infoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            infoCell.addElement(new Phrase("Date: " + request.getInvoiceDate()));
            infoCell.addElement(new Phrase("Invoice No: " + request.getInvoiceNo()));
            invoiceInfoTable.addCell(infoCell);

            document.add(invoiceInfoTable);
            document.add(Chunk.NEWLINE);

            // ✅ 5. Customer Info (with dynamic background)
            PdfPCell custCell = new PdfPCell();
            custCell.addElement(new Phrase("Customer Details", new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD)));
            custCell.addElement(new Phrase(request.getCustomer().getName()));
            custCell.addElement(new Phrase(request.getCustomer().getAddress()));
            custCell.addElement(new Phrase("Phone: " + request.getCustomer().getPhone()));
            custCell.setBackgroundColor(customerBox); // 🔹 color comes from payload
            custCell.setPadding(10);

            PdfPTable custTable = new PdfPTable(1);
            custTable.setWidthPercentage(100);
            custTable.addCell(custCell);
            document.add(custTable);

            document.add(Chunk.NEWLINE);

            // ✅ 6. Items Table (headers get dynamic color)
            PdfPTable itemTable = new PdfPTable(6);
            itemTable.setWidthPercentage(100);
            itemTable.setWidths(new int[] { 4, 1, 2, 2, 1, 2 });

            addHeaderCell(itemTable, "Description", headerColor);
            addHeaderCell(itemTable, "Qty", headerColor);
            addHeaderCell(itemTable, "Rate", headerColor);
            addHeaderCell(itemTable, "Amount", headerColor);
            addHeaderCell(itemTable, "GST%", headerColor);
            addHeaderCell(itemTable, "GST Amt", headerColor);

            // rows
            for (ItemsInfo item : request.getItems()) {
                double gstAmt = (item.getAmount() * item.getGstPercent()) / 100;
                itemTable.addCell(item.getDescription());
                itemTable.addCell(String.valueOf(item.getQty()));
                itemTable.addCell("₹ " + item.getRate());
                itemTable.addCell("₹ " + item.getAmount());
                itemTable.addCell(item.getGstPercent() + "%");
                itemTable.addCell("₹ " + gstAmt);
            }

            document.add(itemTable);

            document.add(Chunk.NEWLINE);

            // 🔹 Totals (right aligned)
            PdfPTable totalsTable = new PdfPTable(1);
            totalsTable.setWidthPercentage(40);
            totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            totalsTable.addCell(getTotalCell("Subtotal: ₹ " + request.getSubTotal()));
            totalsTable.addCell(getTotalCell("GST: ₹ " + request.getGst()));
            totalsTable.addCell(getTotalCell("Total Amount: ₹ " + request.getTotalAmount()));

            document.add(totalsTable);
            document.add(Chunk.NEWLINE);

            // 🔹 Terms & Signature
            document.add(new Paragraph("Terms & Conditions:", new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD)));
            document.add(new Paragraph(request.getTerms()));
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Seal & Signature ____________________"));

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private PdfPCell getTotalCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }

    private BaseColor hexToBaseColor(String hex) {
        try {
            java.awt.Color awtColor = java.awt.Color.decode(hex);
            return new BaseColor(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());
        } catch (Exception e) {
            return BaseColor.WHITE; // default fallback
        }
    }

    private void addHeaderCell(PdfPTable table, String text, BaseColor headerColor) {
        Font headFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(text, headFont));
        cell.setBackgroundColor(headerColor); // 🔹 dynamic color from payload
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6);
        table.addCell(cell);
    }
}
