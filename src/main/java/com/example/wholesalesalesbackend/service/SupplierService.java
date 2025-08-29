package com.example.wholesalesalesbackend.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.wholesalesalesbackend.dto.SupplierRequest;
import com.example.wholesalesalesbackend.model.Supplier;
import com.example.wholesalesalesbackend.repository.InBillRepository;
import com.example.wholesalesalesbackend.repository.SupplierRepository;

@Service
public class SupplierService {

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private InBillRepository billRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    public Supplier addSupplier(Supplier supplier,Long userId,Long clientId) {
        // Set datetimeIST if not provided
        LocalDateTime supplierDateTimeIST;
        ZoneId indiaZone = ZoneId.of("Asia/Kolkata");

        if (supplier.getDatetimeIST() != null) {
            // Treat incoming LocalDateTime as if it is in IST
            ZonedDateTime zonedDateTime = supplier.getDatetimeIST().atZone(indiaZone);
            supplierDateTimeIST = zonedDateTime.toLocalDateTime();
        } else {
            // Use current time in IST
            supplierDateTimeIST = LocalDateTime.now(indiaZone);
        }

        supplier.setDatetimeIST(supplierDateTimeIST);
        supplier.setUserId(userId);
        supplier.setClientId(clientId);
        return supplierRepository.save(supplier);
    }

    public List<Supplier> getAllSuppliers(Long clientId) {
        return supplierRepository.findAllByClientId(clientId);
    }

    public String deleteSupplierByid(Long id) {

        Optional<Supplier> existSupplier = supplierRepository.findById(id);

        if (existSupplier.isPresent()) {

            billRepository.deleteAllBySupplier(existSupplier.get().getName());

            supplierRepository.delete(existSupplier.get());
        }

        return "Deleted !!!";

    }

    public List<Supplier> findAllSuppliers(Long userId, Long clientId) {
        return supplierRepository.findAllByUserIdAndClientId(userId,clientId);

    }

    public String editSupplierByid(Long id, SupplierRequest supplierRequest) {

        Optional<Supplier> existSupplier = supplierRepository.findById(id);

        if (existSupplier.isPresent()) {

            Supplier updatedSupplier = existSupplier.get();
            updatedSupplier.setContactNo(supplierRequest.getContactNo());
            updatedSupplier.setLocation(supplierRequest.getLocation());
            updatedSupplier.setName(supplierRequest.getName());

            supplierRepository.save(updatedSupplier);

        }

        return "Edited !!!";

    }
}