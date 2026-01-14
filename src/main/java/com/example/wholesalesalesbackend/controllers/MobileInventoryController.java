package com.example.wholesalesalesbackend.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.wholesalesalesbackend.dto.MobileInventoryRequest;
import com.example.wholesalesalesbackend.model.MobileInventory;
import com.example.wholesalesalesbackend.service.MobileInventoryService;

@RestController
@RequestMapping("/api/mobile-inventory")
public class MobileInventoryController {

    @Autowired (required = false)
    private MobileInventoryService service;

    /* ===================== */
    /* CREATE */
    /* ===================== */
    @PostMapping("/save")
    public ResponseEntity<MobileInventory> save(
            @RequestBody MobileInventoryRequest request) {
        return ResponseEntity.ok(service.save(request));
    }

    /* ===================== */
    /* GET ALL */
    /* ===================== */
    @GetMapping("/all")
    public ResponseEntity<List<MobileInventory>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    /* ===================== */
    /* UPDATE (EDIT) */
    /* ===================== */
    @PutMapping("/{id}")
    public ResponseEntity<MobileInventory> update(
            @PathVariable Long id,
            @RequestBody MobileInventoryRequest request) {

        return ResponseEntity.ok(service.update(id, request));
    }


    /* ===================== */
    /* DELETE */
    /* ===================== */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
