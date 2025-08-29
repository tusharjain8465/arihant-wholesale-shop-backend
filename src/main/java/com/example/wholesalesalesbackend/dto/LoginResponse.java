package com.example.wholesalesalesbackend.dto;

import com.example.wholesalesalesbackend.model.User;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private User user;
}