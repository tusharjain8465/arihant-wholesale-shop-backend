package com.example.wholesalesalesbackend.service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.wholesalesalesbackend.model.InBill;
import com.example.wholesalesalesbackend.repository.InBillRepository;

@Service
public class InBillService {

    @Autowired
    private InBillRepository inBillRepository;

    public InBill saveInBill(String supplier, Double amount, LocalDateTime date, MultipartFile[] files, Long userId,
            Long clientId)
            throws IOException {
        InBill bill = new InBill();
        bill.setSupplier(supplier);
        bill.setAmount(amount);
        bill.setIsInBill(true);
        bill.setUserId(userId);
        bill.setClientId(clientId);

        bill.setFilter(" " + supplier + " " + amount + " " + " in bill");

        LocalDateTime inDateIST;
        ZoneId indiaZone = ZoneId.of("Asia/Kolkata");

        if (date != null) {
            // Treat incoming LocalDateTime as if it is in IST
            ZonedDateTime zonedDateTime = date.atZone(indiaZone);
            inDateIST = zonedDateTime.toLocalDateTime();
        } else {
            // Use current time in IST
            inDateIST = LocalDateTime.now(indiaZone);
        }

        bill.setDate(inDateIST);

        if (files != null && files.length > 0) {
            String uploadedFiles = Arrays.stream(files)
                    .map(file -> {
                        try {
                            // save file to "uploads" folder (create folder if needed)
                            File dir = new File("uploads");
                            if (!dir.exists())
                                dir.mkdirs();

                            String filePath = "uploads/" + System.currentTimeMillis() + "_"
                                    + file.getOriginalFilename();
                            file.transferTo(new File(filePath));
                            return filePath;
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.joining(","));
            bill.setFileNames(uploadedFiles);
        }

        return inBillRepository.save(bill);
    }

    public InBill saveOutBill(String supplier, Double amount, LocalDateTime date, MultipartFile[] files, Long userId, Long clientId)
            throws IOException {
        InBill bill = new InBill();
        bill.setSupplier(supplier);
        bill.setAmount(amount);
        bill.setIsInBill(false);
        bill.setClientId(clientId);
        bill.setUserId(userId);
        bill.setFilter(" " + supplier + " " + amount + " " + " out bill");

        LocalDateTime inDateIST;
        ZoneId indiaZone = ZoneId.of("Asia/Kolkata");

        if (date != null) {
            // Treat incoming LocalDateTime as if it is in IST
            ZonedDateTime zonedDateTime = date.atZone(indiaZone);
            inDateIST = zonedDateTime.toLocalDateTime();
        } else {
            // Use current time in IST
            inDateIST = LocalDateTime.now(indiaZone);
        }

        bill.setDate(inDateIST);

        if (files != null && files.length > 0) {
            String uploadedFiles = Arrays.stream(files)
                    .map(file -> {
                        try {
                            // save file to "uploads" folder (create folder if needed)
                            File dir = new File("uploads");
                            if (!dir.exists())
                                dir.mkdirs();

                            String filePath = "uploads/" + System.currentTimeMillis() + "_"
                                    + file.getOriginalFilename();
                            file.transferTo(new File(filePath));
                            return filePath;
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.joining(","));
            bill.setFileNames(uploadedFiles);
        }

        return inBillRepository.save(bill);
    }

    public void updateAmount(Long id, Double amount) {

        Optional<InBill> bill = inBillRepository.findById(id);
        if (bill.isPresent()) {

            InBill updatedBill = bill.get();
            String updateFilter = updatedBill.getFilter() + " " + amount + " ";
            updatedBill.setFilter(updateFilter);
            updatedBill.setAmount(amount);

            inBillRepository.save(updatedBill);

        }

    }

    public void deleteBill(Long id) {
        Optional<InBill> bill = inBillRepository.findById(id);
        if (bill.isPresent()) {

            InBill updatedBill = bill.get();

            inBillRepository.delete(updatedBill);

        }

    }

}
