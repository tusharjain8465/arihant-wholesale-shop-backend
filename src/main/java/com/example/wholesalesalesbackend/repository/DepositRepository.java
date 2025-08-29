package com.example.wholesalesalesbackend.repository;

import com.example.wholesalesalesbackend.model.Deposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DepositRepository extends JpaRepository<Deposit, Long> {
        List<Deposit> findByClientId(Long clientId);

        @Query(value = "SELECT t.* FROM public.deposits t " +
                        "WHERE t.client_id = :clientId AND DATE(t.deposit_date) BETWEEN :fromDate AND :toDate " +
                        "ORDER BY t.deposit_date", nativeQuery = true)
        List<Deposit> findByClientIdAndDepositDateBetweenOrderByDepositDateDescCustom(
                        Long clientId,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);

        @Query(value = "SELECT t.* FROM public.deposits t " +
                        "WHERE t.user_id = :userId AND DATE(t.deposit_date) BETWEEN :fromDate AND :toDate " +
                        "ORDER BY t.deposit_date", nativeQuery = true)
        List<Deposit> findByDepositDateBetweenOrderByDepositDateDescCustom(
                        @Param("userId") Long userId,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);

        @Query(value = "SELECT SUM(t.amount) FROM deposits t WHERE t.client_Id = :clientId AND DATE(t.deposit_date) < :fromDate", nativeQuery = true)
        Double getTotalDepositOfSingleClient(@Param("clientId") Long clientId,
                        @Param("fromDate") LocalDateTime fromDate);

        @Query(value = "SELECT SUM(t.amount) FROM deposits t WHERE  t.user_id = :userId AND  DATE(t.deposit_date) < :fromDate", nativeQuery = true)
        Double getTotalDepositOfAllClient(@Param("userId") Long userId,
                        @Param("fromDate") LocalDateTime fromDate);

        @Query(value = "SELECT SUM(t.amount) FROM deposits t WHERE t.client_id in (:clientIds) and t.deposit_date BETWEEN :from AND :to", nativeQuery = true)
        Double findTotalDepositBetweenDatesUserId(@Param("from") LocalDateTime from,
                        @Param("to") LocalDateTime to, @Param ("clientIds") List<Long> clientIds );

        @Query(value = "SELECT SUM(t.amount) FROM deposits t WHERE t.deposit_date BETWEEN :from AND :to  AND t.client_id = :clientId ", nativeQuery = true)
        Double findTotalDepositBetweenDatesAndClientId(@Param("clientId") Long clientId,
                        @Param("from") LocalDateTime from,
                        @Param("to") LocalDateTime to);

        List<Deposit> findAllByUserId(Long userId);

        List<Deposit> findAllByClientIdIn(List<Long> clientIds);

        @Query(value = "SELECT COUNT(t.*) FROM deposits t WHERE  t.user_id = :userId "+

                        "AND t.deposit_date BETWEEN :fromDate AND :toDate ", nativeQuery = true)
        Long getCountOfDeposit(@Param("fromDate") LocalDateTime fromDate,
                        @Param("toDate") LocalDateTime toDate,
                        @Param("userId") Long userId);

        List<Deposit> findAllByClientIdInOrderByDepositDateDesc(List<Long> clientIds);

        void deleteAllByUserId(Long userId);

        void deleteAllByClientId(Long id);
}
