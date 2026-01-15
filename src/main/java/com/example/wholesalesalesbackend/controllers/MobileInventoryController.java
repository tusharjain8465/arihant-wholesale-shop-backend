package com.example.wholesalesalesbackend.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.wholesalesalesbackend.dto.MobilePurchaseInventoryRequest;
import com.example.wholesalesalesbackend.dto.MobileSellInventoryRequest;
import com.example.wholesalesalesbackend.model.MobileInventory;
import com.example.wholesalesalesbackend.service.MobileInventoryService;

@RestController
@RequestMapping("/api/mobile-inventory")
@CrossOrigin(origins = "*")
public class MobileInventoryController {

    @Autowired
    private MobileInventoryService service;

    /* ===================== */
    /* CREATE (SAVE) */
    /* ===================== */
    @PostMapping("/save")
    public ResponseEntity<MobileInventory> save(
            @RequestBody MobilePurchaseInventoryRequest request) {

        MobileInventory saved = service.save(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /* ===================== */
    /* GET ALL */
    /* ===================== */
    @GetMapping("/all")
    public ResponseEntity<List<MobileInventory>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    /* ===================== */
    /* GET BY ID */
    /* ===================== */
    @GetMapping("/{id}")
    public ResponseEntity<MobileInventory> getById(
            @PathVariable Long id) {

        return ResponseEntity.ok(service.findById(id));
    }

    /* ===================== */
    /* UPDATE */
    /* ===================== */
    @PutMapping("/{id}")
    public ResponseEntity<MobileInventory> update(
            @PathVariable Long id,
            @RequestBody MobileSellInventoryRequest request) {

        MobileInventory updated = service.update(id, request);
        return ResponseEntity.ok(updated);
    }

    /* ===================== */
    /* DELETE */
    /* ===================== */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {

        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
