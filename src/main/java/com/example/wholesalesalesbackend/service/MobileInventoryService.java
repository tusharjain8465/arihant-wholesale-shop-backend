package com.example.wholesalesalesbackend.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.wholesalesalesbackend.dto.MobilePurchaseInventoryRequest;
import com.example.wholesalesalesbackend.dto.MobileSellInventoryRequest;
import com.example.wholesalesalesbackend.model.MobileInventory;
import com.example.wholesalesalesbackend.repository.MobileInventoryRepository;

@Service
public class MobileInventoryService {

    @Autowired
    private MobileInventoryRepository repository;

    /* ===================== */
    /* CREATE */
    /* ===================== */
    public MobileInventory save(MobilePurchaseInventoryRequest request) {

        MobileInventory inventory = new MobileInventory();

        inventory.setMobileName(request.mobileName);
        inventory.setImei1(request.imei1);
        inventory.setImei2(request.imei2);
        inventory.setSupplierName(request.supplierName);
        inventory.setPurchaseDate(request.purchaseDate);

        return repository.save(inventory);
    }

    /* ===================== */
    /* UPDATE */
    /* ===================== */
    public MobileInventory update(Long id, MobileSellInventoryRequest request) {

        MobileInventory inventory = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mobile not found"));

        // Update sold info ONLY if provided
        inventory.setSoldTo(request.soldTo);
        inventory.setSoldDate(request.soldDate);
        inventory.setPrice(request.price);

        return repository.save(inventory);
    }

    /* ===================== */
    /* GET ALL */
    /* ===================== */
    public List<MobileInventory> findAll() {
        return repository.findAll();
    }

    /* ===================== */
    /* GET BY ID */
    /* ===================== */
    public MobileInventory findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mobile not found"));
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

   
}
