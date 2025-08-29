package com.example.wholesalesalesbackend.service;

import com.example.wholesalesalesbackend.dto.DepositUpdateRequest;
import com.example.wholesalesalesbackend.model.Deposit;
import com.example.wholesalesalesbackend.model.UserClientFeature;
import com.example.wholesalesalesbackend.repository.DepositRepository;
import com.example.wholesalesalesbackend.repository.UserClientRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DepositService {

    @Autowired
    private DepositRepository depositRepository;

    @Autowired
    private UserClientRepository userClientRepository;

    public Deposit addDeposit(Deposit deposit, Long userId) {
        String addDepositPrefix = "DEPOSIT -> " + deposit.getNote();
        deposit.setNote(addDepositPrefix);

        deposit.setUserId(userId);
        return depositRepository.save(deposit);
    }

    public List<Deposit> getDepositsByClientId(Long userId) {

        List<Long> clientIds = userClientRepository.fetchClientIdsByUserId(userId);

        return depositRepository.findAllByClientIdInOrderByDepositDateDesc(clientIds);
    }

    public Deposit updateDeposit(Long id, DepositUpdateRequest request) {
        Deposit deposit = depositRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Deposit not found"));
        deposit.setAmount(request.getAmount());
        deposit.setNote(request.getNote());
        return depositRepository.save(deposit);
    }

    public void deleteDeposit(Long id) {
        depositRepository.deleteById(id);
    }
}
