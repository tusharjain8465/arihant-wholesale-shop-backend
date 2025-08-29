// package com.example.wholesalesalesbackend.controllers;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;

// import com.example.wholesalesalesbackend.dto.OtpRequest;
// import com.example.wholesalesalesbackend.dto.OtpVerifyRequest;
// import com.example.wholesalesalesbackend.service.OtpService;

// import io.swagger.v3.oas.annotations.parameters.RequestBody;

// @RestController
// @RequestMapping("/api/otp")
// public class OtpController {

//     @Autowired
//     private OtpService otpService;

//     @PostMapping("/send")
//     public ResponseEntity<String> sendOtp(@RequestBody OtpRequest request) {
//         otpService.sendOtp(request.getEmail());
//         return ResponseEntity.ok("OTP sent to email.");
//     }

//     @PostMapping("/verify")
//     public ResponseEntity<String> verifyOtp(@RequestBody OtpVerifyRequest request) {
//         boolean isValid = otpService.verifyOtp(request.getEmail(), request.getOtp());
//         if (isValid)
//             return ResponseEntity.ok("OTP Verified Successfully!");
//         return ResponseEntity.status(400).body("Invalid or expired OTP.");
//     }
// }
