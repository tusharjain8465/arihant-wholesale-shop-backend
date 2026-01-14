package com.example.wholesalesalesbackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.wholesalesalesbackend.dto.MobileInventoryRequest;
import com.example.wholesalesalesbackend.model.MobileInventory;
import com.example.wholesalesalesbackend.repository.MobileInventoryRepository;

import java.time.LocalDate;
import java.util.List;

@Service
public class MobileInventoryService {

    @Autowired
    private MobileInventoryRepository repository;

    /* ===================== */
    /* CREATE */
    /* ===================== */
    public MobileInventory save(MobileInventoryRequest request) {
        MobileInventory inventory = new MobileInventory();
        mapRequestToEntity(request, inventory);
        return repository.save(inventory);
    }

    /* ===================== */
    /* UPDATE */
    /* ===================== */
    public MobileInventory update(Long id, MobileInventoryRequest request) {
        MobileInventory inventory = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mobile not found"));

        mapRequestToEntity(request, inventory);
        return repository.save(inventory);
    }

    /* ===================== */
    /* DELETE */
    /* ===================== */
    public void deleteById(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Mobile not found");
        }
        repository.deleteById(id);
    }

    /* ===================== */
    /* FILTER */
    /* ===================== */
    public List<MobileInventory> filterByDate(LocalDate fromDate, LocalDate toDate) {
        return repository.filterByPurchaseDate(fromDate, toDate);
    }

    public List<MobileInventory> findAll() {
        return repository.findAll();
    }

    public MobileInventory findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mobile not found"));
    }

    /* ===================== */
    /* COMMON MAPPER */
    /* ===================== */
    private void mapRequestToEntity(
            MobileInventoryRequest request,
            MobileInventory inventory) {

        inventory.setMobileName(request.mobileName);
        inventory.setImei1(request.imei1);
        inventory.setImei2(request.imei2);
        inventory.setSupplierName(request.supplierName);
        inventory.setSoldTo(request.soldTo);
        inventory.setPurchaseDate(request.purchaseDate);
        inventory.setSoldDate(request.soldDate);
        inventory.setPrice(request.price);
    }
}
