package com.example.wholesalesalesbackend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.wholesalesalesbackend.model.InBill;

import jakarta.transaction.Transactional;

@Repository
public interface InBillRepository extends JpaRepository<InBill, Long> {

        @Query("""
                        SELECT s FROM InBill s
                        WHERE (:supplier IS NULL OR s.supplier = :supplier)
                          AND (:userId IS NULL OR s.userId = :userId)
                          AND (:clientId IS NULL OR s.clientId = :clientId)
                          AND (s.date >= COALESCE(:startDateTime, s.date))
                          AND (s.date <= COALESCE(:endDateTime, s.date))
                          AND (:searchText IS NULL OR LOWER(s.filter) LIKE LOWER(CONCAT('%', :searchText, '%'))) order by s.date desc
                        """)
        Page<InBill> findAllWithFilters(
                        @Param("supplier") String supplier,
                        @Param("startDateTime") LocalDateTime startDateTime,
                        @Param("endDateTime") LocalDateTime endDateTime,
                        @Param("searchText") String searchText,
                        @Param("userId") Long userId,
                        @Param("clientId") Long clientId,
                        Pageable pageable);

        @Modifying
        @Transactional
        @Query(value = "DELETE FROM public.in_bills WHERE supplier = :supplier", nativeQuery = true)
        int deleteAllBySupplier(@Param("supplier") String supplier);

        List<InBill> findByClientIdInAndDateBetweenAndIsInBillFalse(List<Long> clientIds, LocalDateTime startDate,
                LocalDateTime endDate);

}