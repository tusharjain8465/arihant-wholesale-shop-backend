package com.example.wholesalesalesbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.wholesalesalesbackend.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByMail(String username);

    Optional<User> findByUsernameOrMobileNumberOrMail(String userName, Long mobileNumber, String mail);

    List<User> findByShopOwnerUsername(String username);

    Optional<User> findByUsernameAndShopOwnerUsername(String username, String ownerUserName);


}