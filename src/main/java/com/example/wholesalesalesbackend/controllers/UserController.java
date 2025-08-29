package com.example.wholesalesalesbackend.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.wholesalesalesbackend.dto.SaleEntryDTO;
import com.example.wholesalesalesbackend.model.User;
import com.example.wholesalesalesbackend.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // 🔹 Register new user (default STAFF unless specified)
    @PostMapping("/register")
    public ResponseEntity<User> registerUser(@RequestBody User user) {
        User createdUser = userService.registerUserOrStaff(user);
        return ResponseEntity.ok(createdUser);
    }

    @PostMapping("/add-staff")
    public ResponseEntity<String> addStaff(@RequestBody User user, @RequestParam Long userId) {
        try {
            String result = userService.addStaff(user, userId);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/remove-staff")
    public ResponseEntity<String> removeStaff(@RequestParam String username, @RequestParam Long userId) {
        String deleteStaff = userService.removeStaff(username, userId);
        return ResponseEntity.ok(deleteStaff);
    }

    @GetMapping("/staffs")
    public ResponseEntity<List<User>> getAllStaff(@RequestParam Long userId) {

        List<User> entries = userService.findAllStaff(userId);

        return ResponseEntity.ok(entries);
    }

}
