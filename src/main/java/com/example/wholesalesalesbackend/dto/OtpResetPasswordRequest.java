package com.example.wholesalesalesbackend.dto;

import lombok.Data;

@Data
public class OtpResetPasswordRequest {
    private String userName;
    private String mobileNumber;
    private String otp;
    private String newPassword;

    // Getters and Setters
}