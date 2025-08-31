package com.example.wholesalesalesbackend.controllers;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.wholesalesalesbackend.dto.QuotationRequest;
import com.example.wholesalesalesbackend.model.Client;
import com.example.wholesalesalesbackend.model.Deposit;
import com.example.wholesalesalesbackend.model.SaleEntry;
import com.example.wholesalesalesbackend.model.User;
import com.example.wholesalesalesbackend.model.UserClientFeature;
import com.example.wholesalesalesbackend.repository.DepositRepository;
import com.example.wholesalesalesbackend.repository.SaleEntryRepository;
import com.example.wholesalesalesbackend.repository.UserClientRepository;
import com.example.wholesalesalesbackend.repository.UserRepository;
import com.example.wholesalesalesbackend.service.ClientService;
import com.example.wholesalesalesbackend.service.PdfService;
import com.example.wholesalesalesbackend.service.SaleEntryService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    @Autowired(required = false)
    private PdfService pdfService;

    @Autowired(required = false)
    private ClientService clientService;

    @Autowired(required = false)
    private SaleEntryController saleEntryController;

    @Autowired(required = false)
    SaleEntryRepository saleEntryRepository;

    @Autowired(required = false)
    DepositRepository depositRepository;

    @Autowired(required = false)
    UserRepository userRepository;

    @Autowired(required = false)
    UserClientRepository userClientRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;


    @GetMapping("/send-pdf-backup-on-mail")
    @Async
    public Map<String, Object> sendPdfDailyPerUserOfAllClient() throws IOException, MessagingException {
        List<User> allUsers = userRepository.findAll();
        Map<String, Object> response = new HashMap<>();

        ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");
        LocalDate today = LocalDate.now(INDIA_ZONE);
        LocalDateTime fromDate = today.atStartOfDay();
        LocalDateTime toDate = today.atTime(LocalTime.MAX);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yy"); // safe filename format
        String formattedDate = today.format(formatter);

        for (User eachUser : allUsers) {
            List<UserClientFeature> clients = userClientRepository.findAllByUserId(eachUser.getId());

            // Store all PDFs in a map for this user
            Map<String, byte[]> pdfMap = new HashMap<>();

            // ---------------- PDF per client (existing) ----------------
            for (UserClientFeature eachClient : clients) {
                byte[] pdfBytes = generateSalesPdf(
                        eachClient.getClientId(),
                        fromDate,
                        toDate,
                        null,
                        null,
                        null,
                        eachUser.getId(),
                        null).getBody();

                Client client = clientService.getClientById(eachClient.getClientId());

                // Filename: clientName_date_username.pdf
                String filename = client.getName() + "_" + formattedDate + "_" + eachUser.getUsername() + ".pdf";
                pdfMap.put(filename, pdfBytes);
            }
        
            // ---------------- Send email with all PDFs ----------------
            sendMailWithMultipleAttachments(eachUser.getMail(), pdfMap, formattedDate);
        }

        response.put("Pdf Sent", LocalDateTime.now());
        return response;
    }

    private void sendMailWithMultipleAttachments(String to, Map<String, byte[]> pdfMap, String formattedDate)
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(to);
        helper.setSubject("Daily Sales Reports - " + formattedDate);
        helper.setText("Attached are your sales reports for all clients on " + formattedDate + ".");

        for (Map.Entry<String, byte[]> entry : pdfMap.entrySet()) {
            helper.addAttachment(entry.getKey(), new ByteArrayResource(entry.getValue()));
        }

        mailSender.send(message);
    }

    @GetMapping("/sales")
    public ResponseEntity<byte[]> generateSalesPdf(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime to,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime depositDatetime,
            @RequestParam(required = false, defaultValue = "0") Integer days,
            @RequestParam(required = false) Double oldBalance,
            @RequestParam(required = true) Long userId,
            @RequestParam(required = false, defaultValue = "0") Double depositAmount) throws IOException {

        ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");
        boolean isAllClient = (clientId == null);
        String clientName;
        List<SaleEntry> sales = new ArrayList<>();
        List<Deposit> depoEntries = new ArrayList<>();

        Double depositValueofAll = 0.0;

        // If no dates provided, use current India time
        if (to == null && from == null) {
            LocalDate today = LocalDate.now(INDIA_ZONE);
            to = today.atTime(LocalTime.MAX); // today end of day
            from = today.minusDays(days).atStartOfDay(); // days ago start of day
        }

        LocalDate fromLocalDate = from.toLocalDate();
        LocalDate toLocalDate = to.toLocalDate();

        if (!isAllClient) {
            Client client = clientService.getClientById(clientId);
            clientName = client.getName();

            if (oldBalance == null) {
                oldBalance = saleEntryRepository.getOldBalanceOfClient(clientId, from);
            }

            sales = saleEntryRepository.findByClientIdAndSaleDateBetweenOrderBySaleDateTimeDescCustom(
                    clientId, fromLocalDate, toLocalDate);

            depoEntries = depositRepository.findByClientIdAndDepositDateBetweenOrderByDepositDateDescCustom(clientId,
                    fromLocalDate, toLocalDate);

            depositValueofAll = depositRepository.getTotalDepositOfSingleClient(
                    clientId, from);

            if (oldBalance != null && depositValueofAll != null) {
                oldBalance = oldBalance - depositValueofAll;

            }

        } else {
            clientName = "All_Clients";

            if (oldBalance == null) {
                oldBalance = saleEntryRepository.getOldBalance(from, userId);
            }

            sales = saleEntryRepository.findBySaleDateBetweenOrderBySaleDateTimeDescCustom(
                    fromLocalDate, toLocalDate, userId);

            depoEntries = depositRepository.findByDepositDateBetweenOrderByDepositDateDescCustom(userId, fromLocalDate,
                    toLocalDate);

            depositValueofAll = depositRepository.getTotalDepositOfAllClient(userId, from);

            if (oldBalance != null && depositValueofAll != null) {
                oldBalance = oldBalance - depositValueofAll;

            }

        }

        ByteArrayInputStream bis = pdfService.generateSalesPdf(
                clientName, sales, depoEntries, fromLocalDate, toLocalDate, isAllClient,
                depositAmount, depositDatetime, oldBalance);

        byte[] pdfBytes = bis.readAllBytes();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.builder("inline")
                .filename("sales_report_" + clientName + ".pdf")
                .build());

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/ping")
    public Map<String, Object> pingBackend() {
        Map<String, Object> response = new HashMap<>();
        System.out.println("status --> alive");
        response.put("status", "success");
        response.put("timestamp", LocalDateTime.now());
        return response;
    }

    @GetMapping("/backup")
    public ResponseEntity<byte[]> backupDatabase(
            @RequestParam String username,
            @RequestParam String host,
            @RequestParam int port,
            @RequestParam String database,
            @RequestParam String password) throws IOException, InterruptedException {

        // Use system temporary folder
        String tempDir = System.getProperty("java.io.tmpdir");
        String sqlFile = tempDir + "/" + database + ".backup";
        String zipFile = tempDir + "/" + database + "_backup.zip";

        // Build pg_dump command
        String[] command = {
                "pg_dump",
                "-h", host,
                "-p", String.valueOf(port),
                "-U", username,
                "-F", "c",
                "-b",
                "-v",
                "-f", sqlFile,
                database
        };

        // Set password
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.environment().put("PGPASSWORD", password);
        pb.redirectErrorStream(true);

        // Run backup
        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line); // logs
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("pg_dump failed with exit code " + exitCode);
        }

        // Zip the backup
        try (FileOutputStream fos = new FileOutputStream(zipFile);
                ZipOutputStream zos = new ZipOutputStream(fos);
                FileInputStream fis = new FileInputStream(sqlFile)) {

            ZipEntry zipEntry = new ZipEntry(new File(sqlFile).getName());
            zos.putNextEntry(zipEntry);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) >= 0) {
                zos.write(buffer, 0, length);
            }
            zos.closeEntry();
        }

        // Return zip bytes
        byte[] zipBytes = Files.readAllBytes(new File(zipFile).toPath());

        // Clean up
        new File(sqlFile).delete();
        new File(zipFile).delete();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + database + "_backup.zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zipBytes);
    }

    @PostMapping("/generate-gst-bill")
    public ResponseEntity<byte[]> generateInvoice(@RequestBody QuotationRequest request) {
        ByteArrayInputStream bis = pdfService.generateInvoice(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bis.readAllBytes());
    }

}
