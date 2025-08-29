package com.example.wholesalesalesbackend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.wholesalesalesbackend.dto.ProfitAndSale;
import com.example.wholesalesalesbackend.dto.ProfitAndSaleAndDeposit;
import com.example.wholesalesalesbackend.dto.SaleAttributeUpdateDTO;
import com.example.wholesalesalesbackend.dto.SaleEntryDTO;
import com.example.wholesalesalesbackend.dto.SaleEntryRequestDTO;
import com.example.wholesalesalesbackend.dto.SaleUpdateRequest;
import com.example.wholesalesalesbackend.model.Client;
import com.example.wholesalesalesbackend.model.SaleEntry;
import com.example.wholesalesalesbackend.model.User;
import com.example.wholesalesalesbackend.repository.ClientRepository;
import com.example.wholesalesalesbackend.repository.DepositRepository;
import com.example.wholesalesalesbackend.repository.ProfitAndSaleProjection;
import com.example.wholesalesalesbackend.repository.SaleEntryRepository;
import com.example.wholesalesalesbackend.repository.UserClientRepository;
import com.example.wholesalesalesbackend.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class SaleEntryService {

    @Autowired
    private SaleEntryRepository saleEntryRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepositRepository depositRepository;

    @Autowired
    UserClientRepository userClientRepository;

    public SaleEntry addSaleEntry(SaleEntryRequestDTO dto, Long userId) {

        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new RuntimeException("Client not found"));

        boolean isReturn = Boolean.TRUE.equals(dto.getReturnFlag());

        String accessoryName = dto.getAccessoryName();

        if (accessoryName == null || accessoryName.trim().isEmpty()) {
            accessoryName = "Please Add Accessory";
        }

        accessoryName = isReturn ? "RETURN -> " + accessoryName : "ADD -> " + accessoryName;

        Double totalPrice = Optional.ofNullable(dto.getTotalPrice()).orElse(0.0);
        Double profit = Optional.ofNullable(dto.getProfit()).orElse(0.0);

        if (isReturn) {
            totalPrice = -Math.abs(totalPrice);
            profit = -Math.abs(profit);
        } else {
            totalPrice = Math.abs(totalPrice);
            profit = Math.abs(profit);
        }

        LocalDateTime saleDateTimeInIST;
        ZoneId indiaZone = ZoneId.of("Asia/Kolkata");

        if (dto.getSaleDateTime() != null) {
            // Treat incoming LocalDateTime as if it is in IST
            ZonedDateTime zonedDateTime = dto.getSaleDateTime().atZone(indiaZone);
            saleDateTimeInIST = zonedDateTime.toLocalDateTime();
        } else {
            // Use current time in IST
            saleDateTimeInIST = LocalDateTime.now(indiaZone);
        }

        Optional<User> user = userRepository.findById(userId);

        String searchFilter = accessoryName + " " + " totalprice=" + totalPrice + " profit=" + profit + " "
                + user.get().getUsername();

        SaleEntry saleEntry = SaleEntry.builder()
                .accessoryName(accessoryName)
                .quantity(Optional.ofNullable(dto.getQuantity()).orElse(1))
                .totalPrice(totalPrice)
                .profit(profit)
                .saleDateTime(saleDateTimeInIST)
                .note(dto.getNote())
                .returnFlag(isReturn)
                .client(client)
                .deleteFlag(false)
                .userId(userId)
                .searchFilter(searchFilter)
                .build();

        return saleEntryRepository.save(saleEntry);
    }

    public List<SaleEntry> getSalesByClientAndDateRange(Long clientId, LocalDateTime from, LocalDateTime to,
            Long userId) {

        ZoneId indiaZone = ZoneId.of("Asia/Kolkata");

        // Convert inputs to IST (only if they are not null)
        if (from != null) {
            from = from.atZone(ZoneId.systemDefault())
                    .withZoneSameInstant(indiaZone)
                    .toLocalDateTime();
        }
        if (to != null) {
            to = to.atZone(ZoneId.systemDefault())
                    .withZoneSameInstant(indiaZone)
                    .toLocalDateTime();
        }

        List<SaleEntry> entries = new ArrayList<>();

        if (clientId != null) {
            Client client = clientRepository.findById(clientId)
                    .orElseThrow(() -> new RuntimeException("Client not found"));
            if (from != null && to != null) {
                entries = saleEntryRepository.findByClientAndSaleDateTimeBetweenOrderBySaleDateTimeDesc(client, from,
                        to);
            } else if (from != null) {
                entries = saleEntryRepository.findByClientAndSaleDateTimeAfterOrderBySaleDateTimeDesc(client, from);
            } else if (to != null) {
                entries = saleEntryRepository.findByClientAndSaleDateTimeBeforeOrderBySaleDateTimeDesc(client, to);
            } else {
                entries = saleEntryRepository.findByClientOrderBySaleDateTimeDesc(client);
            }
        }

        else {
            // All clients for this user
            if (from != null && to != null) {
                entries = saleEntryRepository.findByUserIdAndSaleDateTimeBetweenOrderBySaleDateTimeDesc(
                        userId, from, to);
            } else if (from != null) {
                entries = saleEntryRepository.findByUserIdAndSaleDateTimeAfterOrderBySaleDateTimeDesc(
                        userId, from);
            } else if (to != null) {
                entries = saleEntryRepository.findByUserIdAndSaleDateTimeBeforeOrderBySaleDateTimeDesc(
                        userId, to);
            } else {
                entries = saleEntryRepository.findByUserIdOrderBySaleDateTimeDesc(userId);
            }
        }

        return filterNonDeleted(entries);
    }

    public List<SaleEntryDTO> getSalesEntryDTOByClientAndDateRange(Long clientId, LocalDateTime from,
            LocalDateTime to, Long userId) {

        ZoneId indiaZone = ZoneId.of("Asia/Kolkata");

        // Convert incoming times to IST if they are not null
        if (from != null) {
            from = from.atZone(ZoneId.systemDefault())
                    .withZoneSameInstant(indiaZone)
                    .toLocalDateTime();
        }
        if (to != null) {
            to = to.atZone(ZoneId.systemDefault())
                    .withZoneSameInstant(indiaZone)
                    .toLocalDateTime();
        }

        List<SaleEntry> entries = new ArrayList<>();
        List<SaleEntryDTO> dtos = new ArrayList<>();

        if (clientId != null) {
            Client client = clientRepository.findById(clientId)
                    .orElseThrow(() -> new RuntimeException("Client not found"));

            if (from != null && to != null) {
                entries = saleEntryRepository.findByClientAndSaleDateTimeBetweenOrderBySaleDateTimeDesc(client, from,
                        to);
            } else if (from != null) {
                entries = saleEntryRepository.findByClientAndSaleDateTimeAfterOrderBySaleDateTimeDesc(client, from);
            } else if (to != null) {
                entries = saleEntryRepository.findByClientAndSaleDateTimeBeforeOrderBySaleDateTimeDesc(client, to);
            } else {
                entries = saleEntryRepository.findByClientOrderBySaleDateTimeDesc(client);
            }

        } else {
            if (from != null && to != null) {
                entries = saleEntryRepository.findByUserIdAndSaleDateTimeBetweenOrderBySaleDateTimeDesc(
                        userId, from, to);
            } else if (from != null) {
                entries = saleEntryRepository.findByUserIdAndSaleDateTimeAfterOrderBySaleDateTimeDesc(
                        userId, from);
            } else if (to != null) {
                entries = saleEntryRepository.findByUserIdAndSaleDateTimeBeforeOrderBySaleDateTimeDesc(
                        userId, to);
            } else {
                entries = saleEntryRepository.findByUserIdOrderBySaleDateTimeDesc(userId);
            }
        }

        List<SaleEntry> nonDeletedEntries = filterNonDeleted(entries);

        for (SaleEntry sale : nonDeletedEntries) {

            SaleEntryDTO dto = new SaleEntryDTO();
            dto.setId(sale.getId());
            dto.setProfit(sale.getProfit());
            dto.setQuantity(sale.getQuantity());
            dto.setClientName(sale.getClient().getName());
            dto.setSaleDateTime(sale.getSaleDateTime());
            dto.setTotalPrice(sale.getTotalPrice());
            dto.setReturnFlag(sale.getReturnFlag());
            dto.setAccessoryName(sale.getAccessoryName());
            dto.setNote(sale.getNote());

            dtos.add(dto);

        }

        return dtos;

    }

    public SaleEntry updateProfit(SaleAttributeUpdateDTO dto) {
        SaleEntry entry = saleEntryRepository.findById(dto.getSaleEntryId())
                .orElseThrow(() -> new RuntimeException("SaleEntry not found with id: " + dto.getSaleEntryId()));

        entry.setProfit(dto.getProfit());
        entry.setAccessoryName(dto.getAccessory());
        entry.setTotalPrice(dto.getTotalPrice());

        SaleEntry updatedEntry = saleEntryRepository.save(entry);
        List<SaleEntry> entries = Arrays.asList(updatedEntry);
        filterNonDeleted(entries);
        return filterNonDeleted(entries).get(0);

    }

    public List<SaleEntry> getSalesEntryByClient(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));
        return filterNonDeleted(saleEntryRepository.findByClientOrderBySaleDateTimeDesc(client));
    }

    public List<SaleEntryDTO> getSalesEntryDTOByClient(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        List<SaleEntry> entries = filterNonDeleted(saleEntryRepository.findByClientOrderBySaleDateTimeDesc(client));

        List<SaleEntryDTO> dtos = new ArrayList<>();
        for (SaleEntry sale : entries) {

            SaleEntryDTO dto = new SaleEntryDTO();
            dto.setId(sale.getId());
            dto.setProfit(sale.getProfit());
            dto.setQuantity(sale.getQuantity());
            dto.setClientName(sale.getClient().getName());
            dto.setSaleDateTime(sale.getSaleDateTime());
            dto.setTotalPrice(sale.getTotalPrice());
            dto.setReturnFlag(sale.getReturnFlag());
            dto.setNote(sale.getNote());
            dto.setAccessoryName(sale.getAccessoryName());

            dtos.add(dto);

        }

        return dtos;
    }

    @Transactional
    public int updateSalesByClient(Long clientId, Long saleEntryId, SaleUpdateRequest saleUpdateRequest) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        String newAccessoryName = saleUpdateRequest.getAccessoryName();
        LocalDateTime now = saleUpdateRequest.getSaleDateTime();
        Double newTotalPrice = saleUpdateRequest.getTotalPrice();

        return saleEntryRepository.updateSalesByClient(newAccessoryName, now, newTotalPrice, client.getId(),
                saleEntryId);
    }

    public List<SaleEntry> getSalesByDateRange(LocalDateTime from, LocalDateTime to, Long userId) {
        ZoneId indiaZone = ZoneId.of("Asia/Kolkata");

        if (from != null) {
            from = from.atZone(ZoneId.systemDefault())
                    .withZoneSameInstant(indiaZone)
                    .toLocalDateTime();
        }
        if (to != null) {
            to = to.atZone(ZoneId.systemDefault())
                    .withZoneSameInstant(indiaZone)
                    .toLocalDateTime();
        }

        return filterNonDeleted(saleEntryRepository.findByUserIdAndSaleDateTimeBetweenOrderBySaleDateTimeDesc(
                userId, from, to));
    }

    public List<SaleEntry> getAllSales(Long userId) {

        List<Long> clientIds = userClientRepository.fetchClientIdsByUserId(userId);

        ZoneId istZone = ZoneId.of("Asia/Kolkata");
        LocalDateTime fromDate = LocalDateTime.now(istZone).minusDays(3);

        List<SaleEntry> sales = saleEntryRepository
                .findRecentNonDeletedSalesByClientIds(clientIds, fromDate);

        return filterNonDeleted(sales);
    }

    public List<SaleEntry> filterNonDeleted(List<SaleEntry> entries) {
        return entries.stream()
                .filter(entry -> Boolean.FALSE.equals(entry.getDeleteFlag()))
                .collect(Collectors.toList());
    }

    public SaleEntryDTO updateSaleEntry(Long id, SaleEntryDTO updatedEntry) {
        SaleEntry existing = saleEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SaleEntry not found"));

        // Handle accessory name with default and return/add prefix
        String accessoryName = updatedEntry.getAccessoryName();
        if (accessoryName == null || accessoryName.trim().isEmpty()) {
            updatedEntry.setAccessoryName("Please Add Accesory");
        }

        existing
                .setAccessoryName(
                        Boolean.TRUE.equals(updatedEntry.getReturnFlag())
                                ? "RETURN -> " + updatedEntry.getAccessoryName()
                                : "ADD -> " + updatedEntry.getAccessoryName());

        existing.setTotalPrice(
                updatedEntry.getTotalPrice() != null ? updatedEntry.getTotalPrice() : 0.0);
        existing.setProfit(
                updatedEntry.getProfit() != null ? updatedEntry.getProfit() : 0.0);

        existing.setQuantity(updatedEntry.getQuantity());
        existing.setReturnFlag(updatedEntry.getReturnFlag());
        existing.setSaleDateTime(updatedEntry.getSaleDateTime());
        existing.setNote(updatedEntry.getNote());
        existing.setAccessoryName(updatedEntry.getAccessoryName());

        if (updatedEntry.getClientName() != null) {
            Client client = clientRepository.findByName(updatedEntry.getClientName())
                    .orElseThrow(() -> new RuntimeException("Client not found"));
            existing.setClient(client);
        }

        existing = saleEntryRepository.save(existing);

        List<SaleEntry> entries = Arrays.asList(existing);
        SaleEntry entryNonDeleted = filterNonDeleted(entries).get(0);
        // return filterNonDeleted(entries).get(0);

        SaleEntryDTO dto = new SaleEntryDTO();
        dto.setId(id);
        dto.setProfit(entryNonDeleted.getProfit());
        dto.setQuantity(entryNonDeleted.getQuantity());
        dto.setClientName(entryNonDeleted.getClient().getName());
        dto.setSaleDateTime(entryNonDeleted.getSaleDateTime());
        dto.setTotalPrice(entryNonDeleted.getTotalPrice());
        dto.setReturnFlag(entryNonDeleted.getReturnFlag());
        dto.setNote(entryNonDeleted.getNote());
        dto.setAccessoryName(entryNonDeleted.getAccessoryName());

        return dto;
    }

    public String deleteSaleEntry(Long id) {
        SaleEntry existing = saleEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SaleEntry not found"));
        saleEntryRepository.delete(existing);
        return "Deleted !!!";
    }

    public String deleteSoftSaleEntry(Long id) {
        SaleEntry existing = saleEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SaleEntry not found"));

        existing.setDeleteFlag(true);
        saleEntryRepository.save(existing);

        return "Softly Deleted !!!";
    }

    public String restoreSaleEntry(Long id) {
        SaleEntry existing = saleEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SaleEntry not found"));

        existing.setDeleteFlag(false);
        saleEntryRepository.save(existing);

        return "Restored !!!";
    }

    public ProfitAndSaleAndDeposit getTotalProfitByDateRange(LocalDateTime from, LocalDateTime to, Long days,
            Long clientId, Long userId) {

        // If 'days' is provided but no from/to, calculate date range
        if (days != null && from == null && to == null) {
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
            to = today.atTime(LocalTime.MAX); // today end of day
            from = today.minusDays(days).atStartOfDay(); // days ago start of day

        }

        ProfitAndSaleProjection result = null;
        Double deposit = null;

        // If clientId is provided, use client-specific repository method
        if (clientId != null) {
            result = saleEntryRepository.getTotalPriceAndProfitBetweenDatesByClient(from,
                    to, clientId);
            deposit = depositRepository.findTotalDepositBetweenDatesAndClientId(clientId, from, to);

        } else {

            List<Long> clientIds = userClientRepository.fetchClientIdsByUserId(userId);

            result = saleEntryRepository.getTotalPriceAndProfitBetweenDatesUserId(from, to, clientIds);
            deposit = depositRepository.findTotalDepositBetweenDatesUserId(from, to, clientIds);

        }

        Double sale = 0.0;
        Double profit = 0.0;
        Double actualSale = 0.0;
        Double depositValue = 0.0;

        // Handle null projection result
        if (result != null) {
            sale = result.getSale() != null ? result.getSale() : 0.0;
            profit = result.getProfit() != null ? result.getProfit() : 0.0;
            actualSale = sale - profit;
        }

        if (deposit != null) {
            depositValue = deposit;
        }

        return new ProfitAndSaleAndDeposit(sale, actualSale, profit, depositValue);
    }

    public ProfitAndSale getTotalSaleDateRange(LocalDateTime from, LocalDateTime to, Long clientId) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));

        if (from != null) {
            from = today.atStartOfDay(); // days ago start of day

        }
        if (to != null) {
            to = today.atTime(LocalTime.MAX); // today end of day
        }

        ProfitAndSaleProjection result = null;

        boolean isDateRangeProvided = (from != null && to != null);
        boolean isClientProvided = (clientId != null);

        if (isDateRangeProvided && isClientProvided) {
            // Case 1: Both dates and client ID are provided
            result = saleEntryRepository.getTotalPriceAndProfitBetweenDatesByClient(from,
                    to, clientId);
        } else if (isDateRangeProvided) {
            // Case 2: Only dates provided
            result = saleEntryRepository.getTotalPriceAndProfitBetweenDates(from, to);
        } else if (isClientProvided) {
            // Case 3: Only client ID provided
            result = saleEntryRepository.getTotalPriceAndProfitByClient(clientId);
        } else {
            // Case 4: No filters
            result = saleEntryRepository.getTotalPriceAndProfit();
        }

        if (result == null) {
            return new ProfitAndSale(0.0, 0.0);
        }

        return new ProfitAndSale(result.getSale(), result.getProfit());
    }

    public List<SaleEntryDTO> findAllDeleted(Long userId) {

        List<Long> clientIds = userClientRepository.fetchClientIdsByUserId(userId);

        List<SaleEntry> allEntriesDeleted = saleEntryRepository
                .findAllByClient_IdInAndDeleteFlagTrueOrderBySaleDateTimeDesc(clientIds);

        List<SaleEntryDTO> dtos = new ArrayList<>();

        for (SaleEntry sale : allEntriesDeleted) {

            SaleEntryDTO dto = new SaleEntryDTO();
            dto.setId(sale.getId());
            dto.setProfit(sale.getProfit());
            dto.setQuantity(sale.getQuantity());
            dto.setClientName(sale.getClient().getName());
            dto.setSaleDateTime(sale.getSaleDateTime());
            dto.setTotalPrice(sale.getTotalPrice());
            dto.setReturnFlag(sale.getReturnFlag());
            dto.setAccessoryName(sale.getAccessoryName());
            dto.setNote(sale.getNote());

            dtos.add(dto);

        }

        return dtos;

    }

    public List<SaleEntryDTO> findAllByClientDeleted(Long clientId) {

        List<SaleEntry> allEntriesDeleted = saleEntryRepository
                .findAllByClientIdAndDeleteFlagTrueOrderBySaleDateTimeDesc(clientId);

        List<SaleEntryDTO> dtos = new ArrayList<>();

        for (SaleEntry sale : allEntriesDeleted) {

            SaleEntryDTO dto = new SaleEntryDTO();
            dto.setId(sale.getId());
            dto.setProfit(sale.getProfit());
            dto.setQuantity(sale.getQuantity());
            dto.setClientName(sale.getClient().getName());
            dto.setSaleDateTime(sale.getSaleDateTime());
            dto.setTotalPrice(sale.getTotalPrice());
            dto.setReturnFlag(sale.getReturnFlag());
            dto.setAccessoryName(sale.getAccessoryName());
            dto.setNote(sale.getNote());

            dtos.add(dto);

        }

        return dtos;

    }

    public Long getCountOfTrash(Long userId) {
        // TODO Auto-generated method stub

        return saleEntryRepository.getCountOfTrash(userId);

    }

    public Long getCountOfHistory(Long userId) {
        ZoneId istZone = ZoneId.of("Asia/Kolkata");

        LocalDate todayIst = LocalDate.now(istZone);

        LocalDateTime fromDate = todayIst.atStartOfDay(istZone).toLocalDateTime();
        LocalDateTime toDate = todayIst.atTime(LocalTime.MAX).atZone(istZone).toLocalDateTime();

        return saleEntryRepository.getCountOfHistory(fromDate, toDate, userId);
    }

    public Long getCountOfDeposit(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime fromDate = today.atStartOfDay(); // 00:00:00
        LocalDateTime toDate = today.atTime(LocalTime.MAX); // 23:59:59.999999999

        return depositRepository.getCountOfDeposit(fromDate, toDate, userId);
    }

}
