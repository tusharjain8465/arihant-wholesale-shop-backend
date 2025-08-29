package com.example.wholesalesalesbackend.controllers;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
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

import com.example.wholesalesalesbackend.model.Order;
import com.example.wholesalesalesbackend.service.OrderService;
import com.itextpdf.io.exceptions.IOException;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired(required = false)
    private OrderService orderService;

    @PostMapping("/add")
    public Order addOrder(@RequestBody Order order, @RequestParam Long userId, @RequestParam Long clientId) {
        return orderService.addOrder(order, userId, clientId);
    }

    @GetMapping("/all")
    public List<Order> getAllOrders(@RequestParam Long userId, @RequestParam Long clientId,
            @RequestParam String supplier) {
        return orderService.getAllOrders(userId, clientId, supplier);
    }

    @PutMapping("/edit/{id}")
    public Order editOrder(@PathVariable Long id, @RequestBody Order order) {
        return orderService.editOrder(id, order);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
    }

    @GetMapping("/pdf/{id}")
    public ResponseEntity<byte[]> generateOrderPDF(@PathVariable Long id) throws IOException, DocumentException {
        Order order = orderService.findById(id);

        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Create document with A4 page size and margins
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(document, baos);

        document.open();

        // Header: Order
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
        Paragraph headerPara = new Paragraph("Order", headerFont);
        headerPara.setAlignment(Element.ALIGN_CENTER);
        headerPara.setSpacingAfter(20f);
        document.add(headerPara);

        // Supplier Name
        Font supplierFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
        Paragraph supplierPara = new Paragraph(order.getSupplier(), supplierFont);
        document.add(supplierPara);

        Font dateFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);

        // Convert LocalDateTime to Date in IST
        ZoneId zone = ZoneId.of("Asia/Kolkata");
        Date date = Date.from(order.getDateTime().atZone(zone).toInstant());

        // Format the Date
        String formattedDate = new SimpleDateFormat("dd-MM-yyyy").format(date);

        // Add to PDF
        Paragraph datePara = new Paragraph(formattedDate, dateFont);
        datePara.setSpacingBefore(5f);
        document.add(datePara);

        // Notes
        Paragraph notesPara = new Paragraph(order.getNotes(), dateFont);
        notesPara.setSpacingBefore(10f);
        document.add(notesPara);

        // Spacer before footer
        document.add(new Paragraph("\n\n\n\n\n\n"));

        // Footer
        Font footerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Paragraph footerPara = new Paragraph(
                "\n" +
                        "Arihant Mobile Shop Songadh\n" +
                        "Contact on Vishal Jain Mobile No: +91 9537886555",
                footerFont);
        footerPara.setAlignment(Element.ALIGN_CENTER);
        document.add(footerPara);

        document.close();

        // Prepare response
        byte[] pdfBytes = baos.toByteArray();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        // ✅ Force download on phone
        headers.setContentDispositionFormData("attachment", "order_" + order.getId() + ".pdf");
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

}
