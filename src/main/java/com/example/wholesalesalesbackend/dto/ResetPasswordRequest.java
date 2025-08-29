package com.example.wholesalesalesbackend.dto;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String email;
    private String newPassword;
    private String otpCode; // NEW FIELD

}
