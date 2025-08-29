package com.example.wholesalesalesbackend.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.wholesalesalesbackend.model.Client;
import com.example.wholesalesalesbackend.model.SaleEntry;

import jakarta.transaction.Transactional;

@Repository
public interface SaleEntryRepository extends JpaRepository<SaleEntry, Long> {

        // present
        List<SaleEntry> findByClientOrderBySaleDateTimeDesc(Client client);

        // present
        List<SaleEntry> findByClientAndSaleDateTimeBetweenOrderBySaleDateTimeDesc(Client client, LocalDateTime from,
                        LocalDateTime to);

        List<SaleEntry> findByClientAndSaleDateTimeAfterOrderBySaleDateTimeDesc(Client client, LocalDateTime from);

        List<SaleEntry> findByClientAndSaleDateTimeBeforeOrderBySaleDateTimeDesc(Client client, LocalDateTime to);

        // present
        List<SaleEntry> findBySaleDateTimeBetweenOrderBySaleDateTimeDesc(LocalDateTime from, LocalDateTime to);

        @Query(value = "SELECT t.* FROM public.sale_entry t WHERE t.user_id = :userId AND DATE(t.sale_date_time) BETWEEN :fromDate AND :toDate "
                        + //
                        "    ORDER BY t.sale_date_time ", nativeQuery = true)
        List<SaleEntry> findBySaleDateBetweenOrderBySaleDateTimeDescCustom(
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate,
                        @Param("userId") Long userId);

        @Query(value = "SELECT t.* FROM public.sale_entry t WHERE t.client_id =:clientId AND DATE(t.sale_date_time) BETWEEN :fromDate AND :toDate "
                        + //
                        "    ORDER BY t.sale_date_time ", nativeQuery = true)
        List<SaleEntry> findByClientIdAndSaleDateBetweenOrderBySaleDateTimeDescCustom(
                        @Param("clientId") Long clientId,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);

        // present
        List<SaleEntry> findAllByOrderBySaleDateTimeDesc();

        @Query(value = "SELECT SUM(total_price) AS sale, SUM(profit) AS profit " +
                        "FROM sale_entry WHERE client_id in (:clientIds) and delete_flag = false and sale_date_time BETWEEN :from AND :to", nativeQuery = true)
        ProfitAndSaleProjection getTotalPriceAndProfitBetweenDatesUserId(@Param("from") LocalDateTime from,
                        @Param("to") LocalDateTime to, @Param ("clientIds") List<Long> clientIds);


          @Query(value = "SELECT SUM(total_price) AS sale, SUM(profit) AS profit " +
                        "FROM sale_entry WHERE delete_flag = false and sale_date_time BETWEEN :from AND :to", nativeQuery = true)
        ProfitAndSaleProjection getTotalPriceAndProfitBetweenDates(@Param("from") LocalDateTime from,
                        @Param("to") LocalDateTime to);


        @Query(value = "SELECT SUM(total_price) AS sale, SUM(profit) AS profit " +
                        "FROM sale_entry WHERE client_id= :clientId and delete_flag = false and sale_date_time BETWEEN :from AND :to", nativeQuery = true)
        ProfitAndSaleProjection getTotalPriceAndProfitBetweenDatesByClient(@Param("from") LocalDateTime from,
                        @Param("to") LocalDateTime to,
                        @Param("clientId") Long clientId);

        @Modifying
        @Transactional
        @Query(value = "UPDATE sale_entry SET " +
                        "accessory_name = :accessoryName, " +
                        "sale_date_time = :saleDateTime, " +
                        "total_price = :totalPrice " +
                        "WHERE client_id = :clientId AND " +
                        "id = :saleEntryId", nativeQuery = true)
        int updateSalesByClient(
                        @Param("accessoryName") String accessoryName,
                        @Param("saleDateTime") LocalDateTime saleDateTime,
                        @Param("totalPrice") double totalPrice,
                        @Param("clientId") Long clientId,
                        @Param("saleEntryId") Long saleEntryId);

        List<SaleEntry> findBySaleDateTimeAfterOrderBySaleDateTimeDesc(LocalDateTime from);

        List<SaleEntry> findBySaleDateTimeBeforeOrderBySaleDateTimeDesc(LocalDateTime to);

        @Query(value = "SELECT SUM(total_price) AS sale, SUM(profit) AS profit " +
                        "FROM sale_entry WHERE client_id = :clientId", nativeQuery = true)
        ProfitAndSaleProjection getTotalPriceAndProfitByClient(@Param("clientId") Long clientId);

        @Query(value = "SELECT SUM(total_price) AS sale, SUM(profit) AS profit FROM sale_entry where t.delete_flag = false", nativeQuery = true)
        ProfitAndSaleProjection getTotalPriceAndProfit();

        @Query(value = "SELECT SUM(t.total_price) FROM sale_entry t WHERE t.delete_flag = false AND t.user_id = :userId AND DATE(t.sale_date_time) < :fromDate ", nativeQuery = true)
        Double getOldBalance(
                        @Param("fromDate") LocalDateTime fromDate, @Param("userId") Long userId);

        @Query(value = "SELECT SUM(t.total_price) FROM sale_entry t WHERE t.client_id =:clientId AND t.delete_flag = false AND DATE(t.sale_date_time) < :fromDate ", nativeQuery = true)
        Double getOldBalanceOfClient(@Param("clientId") Long clientId,
                        @Param("fromDate") LocalDateTime fromDate);

        @Modifying
        @Query(value = "DELETE FROM sale_entry WHERE client_id = :clientId", nativeQuery = true)
        void deleteByClientId(@Param("clientId") Long clientId);

        // for graph Data

        List<SaleEntry> findBySaleDateTimeBetween(LocalDateTime start, LocalDateTime end);

        @Query("SELECT s FROM SaleEntry s WHERE EXTRACT(YEAR FROM s.saleDateTime) = :year")
        List<SaleEntry> findByYear(int year); // 1️⃣ By clientId

        Page<SaleEntry> findByClientId(Long clientId, Pageable pageable);

        // 2️⃣ By saleDateTime range
        Page<SaleEntry> findBySaleDateTimeBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

        // 3️⃣ By accessoryName containing (partial match, case-insensitive)
        Page<SaleEntry> findByAccessoryNameContainingIgnoreCase(String accessoryName, Pageable pageable);

        // 4️⃣ By clientId and saleDateTime range
        Page<SaleEntry> findByClientIdAndSaleDateTimeBetween(Long clientId, LocalDateTime start, LocalDateTime end,
                        Pageable pageable);

        // 5️⃣ By clientId and accessoryName (partial, ignore case)
        Page<SaleEntry> findByClientIdAndAccessoryNameContainingIgnoreCase(Long clientId, String accessoryName,
                        Pageable pageable);

        // 6️⃣ By saleDateTime range and accessoryName (partial, ignore case)
        Page<SaleEntry> findBySaleDateTimeBetweenAndAccessoryNameContainingIgnoreCase(LocalDateTime start,
                        LocalDateTime end, String accessoryName, Pageable pageable);

        // 7️⃣ By clientId, saleDateTime range, and accessoryName (partial, ignore case)
        Page<SaleEntry> findByClientIdAndSaleDateTimeBetweenAndAccessoryNameContainingIgnoreCase(
                        Long clientId,
                        LocalDateTime start,
                        LocalDateTime end,
                        String accessoryName,
                        Pageable pageable);

        @Query(value = """
                        SELECT *
                        FROM sale_entry s
                        WHERE (:clientId IS NULL OR s.client_id = :clientId)
                          AND (s.sale_date_time >= COALESCE(:startDateTime, s.sale_date_time))
                          AND (s.sale_date_time <= COALESCE(:endDateTime, s.sale_date_time))
                          AND (:searchText IS NULL OR LOWER(s.search_filter) LIKE LOWER(CONCAT('%', :searchText, '%')))
                          AND s.delete_flag = FALSE
                        """, countQuery = """
                        SELECT COUNT(*)
                        FROM sale_entry s
                        WHERE (:clientId IS NULL OR s.client_id = :clientId)
                          AND (s.sale_date_time >= COALESCE(:startDateTime, s.sale_date_time))
                          AND (s.sale_date_time <= COALESCE(:endDateTime, s.sale_date_time))
                          AND (:searchText IS NULL OR LOWER(s.search_filter) LIKE LOWER(CONCAT('%', :searchText, '%')))
                          AND s.delete_flag = FALSE
                        """, nativeQuery = true)
        Page<SaleEntry> findAllWithFiltersWithClientId(
                        @Param("clientId") Long clientId,
                        @Param("startDateTime") LocalDateTime startDateTime,
                        @Param("endDateTime") LocalDateTime endDateTime,
                        @Param("searchText") String searchText,
                        Pageable pageable);

        @Query(value = """
                        SELECT *
                        FROM sale_entry s
                        WHERE (s.sale_date_time >= COALESCE(:startDateTime, s.sale_date_time))
                          AND (s.sale_date_time <= COALESCE(:endDateTime, s.sale_date_time))
                          AND (:searchText IS NULL OR LOWER(s.search_filter) LIKE LOWER(CONCAT('%', :searchText, '%')))
                          AND s.delete_flag = FALSE
                          AND s.client_id IN (
                              SELECT client_id
                              FROM users_clients
                              WHERE user_id = :userId
                          )
                        """, countQuery = """
                        SELECT COUNT(*)
                        FROM sale_entry s
                        WHERE (s.sale_date_time >= COALESCE(:startDateTime, s.sale_date_time))
                          AND (s.sale_date_time <= COALESCE(:endDateTime, s.sale_date_time))
                          AND (:searchText IS NULL OR LOWER(s.search_filter) LIKE LOWER(CONCAT('%', :searchText, '%')))
                          AND s.delete_flag = FALSE
                          AND s.client_id IN (
                              SELECT client_id
                              FROM users_clients
                              WHERE user_id = :userId
                          )
                        """, nativeQuery = true)
        Page<SaleEntry> findAllWithFiltersWithUserId(
                        @Param("startDateTime") LocalDateTime startDateTime,
                        @Param("endDateTime") LocalDateTime endDateTime,
                        @Param("searchText") String searchText,
                        @Param("userId") Long userId,
                        Pageable pageable);

        List<SaleEntry> findAllByDeleteFlagTrueOrderBySaleDateTimeDesc();

        List<SaleEntry> findAllByClientIdAndDeleteFlagTrueOrderBySaleDateTimeDesc(Long clientId);

        // --- Client + UserId filters ---
        List<SaleEntry> findByClientAndUserIdAndSaleDateTimeBetweenOrderBySaleDateTimeDesc(
                        Client client, Long userId, LocalDateTime from, LocalDateTime to);

        List<SaleEntry> findByClientAndUserIdAndSaleDateTimeAfterOrderBySaleDateTimeDesc(
                        Client client, Long userId, LocalDateTime from);

        List<SaleEntry> findByClientAndUserIdAndSaleDateTimeBeforeOrderBySaleDateTimeDesc(
                        Client client, Long userId, LocalDateTime to);

        List<SaleEntry> findByClientAndUserIdOrderBySaleDateTimeDesc(Client client, Long userId);

        // --- Only UserId filters (all clients for a user) ---
        List<SaleEntry> findByUserIdAndSaleDateTimeBetweenOrderBySaleDateTimeDesc(
                        Long userId, LocalDateTime from, LocalDateTime to);

        List<SaleEntry> findByUserIdAndSaleDateTimeAfterOrderBySaleDateTimeDesc(
                        Long userId, LocalDateTime from);

        List<SaleEntry> findByUserIdAndSaleDateTimeBeforeOrderBySaleDateTimeDesc(
                        Long userId, LocalDateTime to);

        List<SaleEntry> findByUserIdOrderBySaleDateTimeDesc(Long userId);

        List<SaleEntry> findByClient_IdAndSaleDateTimeBetween(
                        Long clientId,
                        LocalDateTime startDate,
                        LocalDateTime endDate);

        List<SaleEntry> findAllByClient_IdInAndSaleDateTimeBetween(
                        List<Long> clientIds,
                        LocalDateTime start,
                        LocalDateTime end);

        List<SaleEntry> findAllByClient_IdInAndDeleteFlagTrueOrderBySaleDateTimeDesc(List<Long> clientIds);

        List<SaleEntry> findByClientIdInOrderBySaleDateTimeDesc(List<Long> clientds);

        @Query("SELECT s FROM SaleEntry s " +
                        "WHERE s.client.id IN :clientIds " +
                        "AND s.deleteFlag = false " +
                        "AND s.saleDateTime >= :fromDate " +
                        "ORDER BY s.saleDateTime DESC")
        List<SaleEntry> findRecentNonDeletedSalesByClientIds(
                        @Param("clientIds") List<Long> clientIds,
                        @Param("fromDate") LocalDateTime fromDate);

        List<SaleEntry> findAllByClient_IdInAndDeleteFlagFalseAndSaleDateTimeBetween(List<Long> clientIds,
                        LocalDateTime monthStart, LocalDateTime monthEnd);

        @Query(value = "select count(*) from public.sale_entry where delete_flag = true and client_id in (select client_id from public.users_clients where user_id = :userId) ", nativeQuery = true)
        Long getCountOfTrash(@Param("userId") Long userId);

        @Query(value = "SELECT COUNT(*) " +
                        "FROM public.sale_entry t " +
                        "WHERE t.delete_flag = false " +
                        "AND t.sale_date_time BETWEEN :fromDate AND :toDate " +
                        "AND t.client_id IN (SELECT client_id FROM public.users_clients WHERE user_id = :userId)", nativeQuery = true)
        Long getCountOfHistory(@Param("fromDate") LocalDateTime fromDate,
                        @Param("toDate") LocalDateTime toDate,
                        @Param("userId") Long userId);

}
