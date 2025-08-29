package com.example.wholesalesalesbackend.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.wholesalesalesbackend.model.User;
import com.example.wholesalesalesbackend.model.UserClientFeature;
import com.example.wholesalesalesbackend.repository.UserClientRepository;
import com.example.wholesalesalesbackend.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserClientRepository userclientRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Register new user
    public User registerUserOrStaff(User user) {

        // If no owner credentials provided → Register new Owner
        if ((user.getShopOwnerUsername() == null || user.getShopOwnerUsername().isBlank()) &&
                (user.getOwnerPassword() == null || user.getOwnerPassword().isBlank())) {

            user.setPassword(passwordEncoder.encode(user.getPassword()));

            if (user.getRoles() == null || user.getRoles().isEmpty()) {
                user.setRoles(new HashSet<>());
                user.getRoles().add("OWNER");
            }

            return userRepository.save(user);
        }

        // Else: Register as Staff
        Optional<User> ownerOpt = userRepository.findByUsername(user.getShopOwnerUsername());

        if (ownerOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid shop owner username");
        }

        User owner = ownerOpt.get();

        if (!passwordEncoder.matches(user.getOwnerPassword(), owner.getPassword())) {
            throw new IllegalArgumentException("Invalid shop owner password");
        }

        if (!owner.getRoles().contains("OWNER")) {
            throw new IllegalStateException("Provided user is not an OWNER");
        }

        // Create staff account
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            user.setRoles(new HashSet<>());
            user.getRoles().add("STAFF");
        }

        User savedUser = userRepository.save(user);

        List<UserClientFeature> userClientFeatures = userclientRepository.findAllByUserId(owner.getId());
        for (UserClientFeature clients : userClientFeatures) {

            UserClientFeature staffClient = new UserClientFeature();
            staffClient.setClientId(clients.getClientId());
            staffClient.setUserId(savedUser.getId());
            userclientRepository.save(staffClient);
        }

        return savedUser;
    }

    // Find user by ID
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    @Transactional
    public String addStaff(User user, Long userId) {

        // Validate owner
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Owner not found with id " + userId));

        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Ensure staff role
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            user.setRoles(new HashSet<>());
            user.getRoles().add("STAFF");
        }

        // Link staff to shop owner
        user.setShopOwnerUsername(owner.getUsername());

        // Save staff
        User savedUser;
        try {
            savedUser = userRepository.save(user);
        } catch (Exception e) {
            throw new RuntimeException("Error adding staff: " + e.getMessage());
        }

        // Copy all client features from owner to staff
        List<UserClientFeature> userClientFeatures = userclientRepository.findAllByUserId(userId);
        for (UserClientFeature clients : userClientFeatures) {
            UserClientFeature staffClient = new UserClientFeature();
            staffClient.setClientId(clients.getClientId());
            staffClient.setUserId(savedUser.getId());
            userclientRepository.save(staffClient);
        }

        return "Staff " + savedUser.getUsername() + " added successfully under owner " + owner.getUsername();
    }

    @Modifying
    @Transactional
    public String removeStaff(String username, Long userId) {

        Optional<User> owner = userRepository.findById(userId);
        Optional<User> staff = userRepository.findByUsername(username);

        if (owner.isEmpty() || staff.isEmpty()) {
            return "Owner or staff not found";
        }

        String ownerUserName = owner.get().getUsername();

        Optional<User> ownerAndStaff = userRepository.findByUsernameAndShopOwnerUsername(username, ownerUserName);

        if (ownerAndStaff.isEmpty()) {
            return "Staff does not belong to this owner";
        }

        // Delete UserClientFeature if present
        List<UserClientFeature> userclient = userclientRepository.findByUserId(staff.get().getId());

        for (UserClientFeature userClientFeature : userclient) {

            userclientRepository.delete(userClientFeature);
        }

        // Now delete staff
        userRepository.deleteById(ownerAndStaff.get().getId());

        return "yes";
    }

    public List<User> findAllStaff(Long userId) {

        Optional<User> owner = userRepository.findById(userId);

        String ownerUserName = owner.get().getUsername();

        return userRepository.findByShopOwnerUsername(ownerUserName);

    }
}