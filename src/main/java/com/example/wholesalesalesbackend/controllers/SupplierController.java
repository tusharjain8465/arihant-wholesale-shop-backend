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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.wholesalesalesbackend.dto.SupplierRequest;
import com.example.wholesalesalesbackend.model.Supplier;
import com.example.wholesalesalesbackend.service.SupplierService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    @Autowired(required = false)
    private SupplierService supplierService;

    @PostMapping("/add")
    public ResponseEntity<String> addSupplier(@Valid @RequestBody Supplier supplier ,@RequestParam Long clientId,@RequestParam Long userId) {

        supplierService.addSupplier(supplier,userId,clientId);
        return ResponseEntity.ok("Added");
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteSupplierByid(@PathVariable Long id) {
        String output = supplierService.deleteSupplierByid(id);
        return ResponseEntity.ok(output);
    }

    @PutMapping("/edit/{id}")
    public ResponseEntity<String> editSupplierByid(@PathVariable Long id,
            @RequestBody SupplierRequest supplierRequest) {
        String output = supplierService.editSupplierByid(id, supplierRequest);
        return ResponseEntity.ok(output);
    }

    @GetMapping("/all-supplier")
    public ResponseEntity<List<Supplier>> findAllSuppliers(@RequestParam Long userId , @RequestParam  Long clientId) {
        List<Supplier> entries = supplierService.findAllSuppliers(userId,clientId);
        return ResponseEntity.ok(entries);
    }


}
