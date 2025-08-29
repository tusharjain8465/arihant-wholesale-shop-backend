package com.example.wholesalesalesbackend.controllers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.wholesalesalesbackend.dto.EmailRequest;
import com.example.wholesalesalesbackend.dto.JwtUtil;
import com.example.wholesalesalesbackend.dto.LoginRequest;
import com.example.wholesalesalesbackend.dto.LoginResponse;
import com.example.wholesalesalesbackend.dto.ResetPassowrd;
import com.example.wholesalesalesbackend.dto.ResetPasswordRequest;
import com.example.wholesalesalesbackend.model.OtpVerification;
import com.example.wholesalesalesbackend.model.User;
import com.example.wholesalesalesbackend.repository.OtpRepository;
import com.example.wholesalesalesbackend.repository.UserRepository;
import com.example.wholesalesalesbackend.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // === LOGIN ===
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // find user by username/mail/mobile
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername())
                .or(() -> userRepository.findByUsername(request.getUsername()));

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("User not found");
        }

        User user = userOpt.get();

        // check password manually
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        // generate token with roles
        String token = jwtUtil.generateToken(user.getUsername(), user.getRoles());

        return ResponseEntity.ok(new LoginResponse(token, user));
    }

    /** RESET PASSWORD WITH OLD PASSWORD */
    @PutMapping("/reset-password")
    public ResponseEntity<String> updateUserPassword(@RequestBody ResetPassowrd request) {
        Optional<User> user = userRepository.findByUsernameOrMobileNumberOrMail(
                request.getUserName(),
                request.getMobileNumber(),
                request.getMail());

        if (!user.isPresent()) {
            return ResponseEntity.status(404).body("User Not Found!!!");
        }

        User existingUser = user.get();

        if (!passwordEncoder.matches(request.getOldPassword(), existingUser.getPassword())) {
            return ResponseEntity.status(400).body("Old Password is Wrong!!!");
        }

        existingUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(existingUser);

        return ResponseEntity.status(200).body("Password updated Successfully!!!");
    }

    /** RESET PASSWORD WITH EMAIL + OTP */
    @PostMapping("/reset-password-mail")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        Optional<User> userOptional = userRepository.findByMail(request.getEmail());
        if (!userOptional.isPresent()) {
            return ResponseEntity.status(404).body("User not found.");
        }

        Optional<OtpVerification> otp = otpRepository.findTopByEmailOrderByIdDesc(request.getEmail());

        if (!otp.isPresent()) {
            return ResponseEntity.status(404).body("OTP not found. Please request a new OTP.");
        }

        ZoneId indiaZone = ZoneId.of("Asia/Kolkata");
        if (Duration.between(otp.get().getCreatedAt(), LocalDateTime.now(indiaZone)).toMinutes() > 10) {
            return ResponseEntity.status(400).body("OTP expired. Please request a new one.");
        }

        if (!otp.get().getOtp().equals(request.getOtpCode())) {
            return ResponseEntity.status(400).body("Invalid OTP");
        }

        User user = userOptional.get();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok("Password reset successfully.");
    }

    /** SEND OTP FOR FORGOT PASSWORD */
    @PostMapping("/send-otp")
    public ResponseEntity<String> sendOtp(@RequestBody EmailRequest request) {
        String otp = String.format("%06d", new Random().nextInt(999999));

        OtpVerification otpEntity = new OtpVerification();
        otpEntity.setEmail(request.getEmail());
        otpEntity.setOtp(otp);
        otpEntity.setVerified(false);
        otpEntity.setCreatedAt(LocalDateTime.now()); // ensure timestamp is saved
        otpRepository.save(otpEntity);

        // Send mail
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(request.getEmail());
        message.setSubject("OTP Verification");
        message.setText("Your OTP is: " + otp);
        mailSender.send(message);

        return ResponseEntity.ok("OTP sent to email.");
    }

    /** LOGOUT */
    @GetMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        request.getSession().invalidate();
        return ResponseEntity.ok("Logged out successfully");
    }
}
