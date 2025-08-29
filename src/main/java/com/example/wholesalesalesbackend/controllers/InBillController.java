package com.example.wholesalesalesbackend.controllers;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
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

        Page<InBill> entries = inBillRepository.findAllWithFilters(supplier, startDateTime, endDateTime, normalizedSearch,
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
