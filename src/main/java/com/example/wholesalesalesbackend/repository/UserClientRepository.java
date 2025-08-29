package com.example.wholesalesalesbackend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.wholesalesalesbackend.model.User;
import com.example.wholesalesalesbackend.model.UserClientFeature;

import jakarta.transaction.Transactional;

public interface UserClientRepository extends JpaRepository<UserClientFeature, Long> {

    List<UserClientFeature> findAllByUserId(Long userId);

    @Query(value = "SELECT t.client_id FROM users_clients t WHERE t.user_id = :userId", nativeQuery = true)
    List<Long> fetchClientIdsByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query(value = "delete from public.users_clients where id = :id", nativeQuery = true)
    void deleteFeatureById(@Param("id") Long id);

    Optional<UserClientFeature> findByClientIdAndUserId(Long id, Long userId);

    void deleteAllByclientId(Long id);

    List<UserClientFeature> findByUserId(Long id);


}
