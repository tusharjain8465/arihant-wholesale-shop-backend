package com.example.wholesalesalesbackend.dto;

public class OtpUtil {
    public static String generateOtp() {
        return String.valueOf((int)(Math.random() * 9000) + 1000); // 4-digit OTP
    }
}
