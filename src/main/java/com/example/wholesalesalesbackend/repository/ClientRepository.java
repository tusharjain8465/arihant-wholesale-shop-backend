package com.example.wholesalesalesbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.wholesalesalesbackend.model.Client;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByName(String name);

    boolean existsByName(String name);

    @Query(value = "SELECT t.* FROM public.clients t ORDER BY t.name", nativeQuery = true)
    List<Client> findAllOrderByNameDesc();

    List<Client> findAllByUserIdOrderByNameDesc(Long userId);

    List<Client> findAllByIdIn(List<Long> clientIds);

    @Query(value = "SELECT * FROM clients WHERE id IN (:clientIds) ORDER BY name ASC", nativeQuery = true)
    List<Client> findAllClientsOnlyNative(@Param("clientIds") List<Long> clientIds);

}
