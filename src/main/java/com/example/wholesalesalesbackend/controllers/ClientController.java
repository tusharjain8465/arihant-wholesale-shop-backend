package com.example.wholesalesalesbackend.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.wholesalesalesbackend.dto.ClientCreateRequest;
import com.example.wholesalesalesbackend.model.Client;
import com.example.wholesalesalesbackend.service.ClientService;

import jakarta.validation.Valid;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/clients")
public class ClientController {

    @Autowired(required = false)
    private ClientService clientService;

    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addClient(
            @RequestBody ClientCreateRequest request,
            @RequestParam Long userId) {

        Client newClient = Client.builder()
                .name(request.getName())
                .location(request.getLocation())
                .contact(request.getContact())
                .build();

        Client saved = clientService.addClient(newClient, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Client added successfully");
        response.put("client", saved);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Client>> getAllClients(@RequestParam Long userId) {
        return ResponseEntity.ok(clientService.getAllClientsWithoutSales(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Client> getClientById(@PathVariable Long id, @RequestParam Long userId) {
        return ResponseEntity.ok(clientService.getClientById(id));
    }

    @PutMapping("/edit/{id}")
    public ResponseEntity<Client> updateClient(@PathVariable Long id, @RequestParam Long userId,
            @RequestBody @Valid ClientCreateRequest request) {
        Client updatedClient = clientService.updateClient(id, request);
        return ResponseEntity.ok(updatedClient);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteClient(@PathVariable Long id, @RequestParam Long userId) {
        String output = clientService.deleteClient(id, userId);
        return ResponseEntity.ok(output);
    }

}
