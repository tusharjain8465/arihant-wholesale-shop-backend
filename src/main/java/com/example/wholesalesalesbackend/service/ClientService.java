package com.example.wholesalesalesbackend.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.wholesalesalesbackend.dto.ClientCreateRequest;
import com.example.wholesalesalesbackend.model.Client;
import com.example.wholesalesalesbackend.model.User;
import com.example.wholesalesalesbackend.model.UserClientFeature;
import com.example.wholesalesalesbackend.repository.ClientRepository;
import com.example.wholesalesalesbackend.repository.DepositRepository;
import com.example.wholesalesalesbackend.repository.SaleEntryRepository;
import com.example.wholesalesalesbackend.repository.UserClientRepository;
import com.example.wholesalesalesbackend.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    SaleEntryRepository saleEntryRepository;

    @Autowired
    UserClientRepository userClientRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    DepositRepository depositRepository;

    @Transactional
    public Client addClient(Client client, Long userId) {
        removeUserClientFeatureDuplicates();

        // check duplicate client name
        if (clientRepository.existsByName(client.getName())) {
            throw new RuntimeException("Client already exists with name: " + client.getName());
        }

        // set userId before saving
        client.setUserId(userId);
        Client savedClient = clientRepository.save(client);

        Optional<UserClientFeature> existing = userClientRepository.findByClientIdAndUserId(savedClient.getId(),
                userId);

        if (!existing.isPresent()) {
            UserClientFeature userClientFeature = new UserClientFeature();
            userClientFeature.setClientId(savedClient.getId());
            userClientFeature.setUserId(userId);
            userClientRepository.save(userClientFeature);

            List<User> staffUsers = userRepository
                    .findByShopOwnerUsername(userRepository.findById(userId).get().getUsername());

            for (User staff : staffUsers) {
                UserClientFeature userClientFeatureStaff = new UserClientFeature();
                userClientFeatureStaff.setClientId(savedClient.getId());
                userClientFeatureStaff.setUserId(staff.getId());
                userClientRepository.save(userClientFeatureStaff);
            }

        }

        removeUserClientFeatureDuplicates();
        return savedClient;
    }

    @Transactional
    public void removeUserClientFeatureDuplicates() {
        List<UserClientFeature> allFeatures = userClientRepository.findAll();

        // use a Set to track unique combinations
        Set<String> uniqueKeys = new HashSet<>();

        for (UserClientFeature feature : allFeatures) {
            String key = feature.getUserId() + "-" + feature.getClientId();
            if (uniqueKeys.contains(key)) {
                // duplicate → delete
                userClientRepository.deleteFeatureById(feature.getId());
            } else {
                uniqueKeys.add(key);
            }
        }
    }

    // public Client addClient(Client client, Long userId) {
    // if (clientRepository.existsByName(client.getName())) {
    // throw new RuntimeException("Client already exists with name: " +
    // client.getName());
    // }

    // // set userId in client before saving
    // client.setUserId(userId);

    // Client savedClient = clientRepository.save(client);

    // List<UserClientFeature> existUserClients =
    // userClientRepository.findAllByUserIdAndOwnerIdAndClientId(userId,
    // savedClient.getId());

    // List<Feature> features = featureRepository.findAll();
    // if (existUserClients.isEmpty()) {

    // for (Feature feature : features) {

    // List<UserClientFeature> existUserClientsWithFeatures = userClientRepository
    // .findAllByUserIdAndOwnerIdAndFeatureId(
    // userId,
    // feature.getId());

    // for (UserClientFeature userClientFeature : existUserClientsWithFeatures) {
    // if (userClientFeature.getClientId() != null &&
    // userClientFeature.getFeatureId() != null) {
    // userClientFeature.setClientId(savedClient.getId());
    // userClientRepository.save(userClientFeature);
    // } else if (userClientFeature.getClientId() == null) {
    // UserClientFeature newUserClientFeature = new UserClientFeature();
    // newUserClientFeature.setClientId(client.getId());
    // newUserClientFeature.setFeatureId();
    // }

    // }

    // }

    // }

    // return savedClient;
    // }


       public List<Client> getAllClientsWithoutSales(Long userId) {
        List<Long> clientIds = userClientRepository.fetchClientIdsByUserId(userId);
        return clientRepository.findAllClientsOnlyNative(clientIds);
    }


    public Client getClientById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found with id: " + id));
    }

    public Client getClientByName(String name) {
        return clientRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Client not found with name: " + name));
    }

    public Client updateClient(Long id, ClientCreateRequest request) {
        Client existing = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        existing.setName(request.getName());
        existing.setLocation(request.getLocation());
        existing.setContact(request.getContact());

        return clientRepository.save(existing);
    }

    @Transactional
    public String deleteClient(Long id, Long userId) {
        Client existing = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        saleEntryRepository.deleteByClientId(existing.getId());
        userClientRepository.deleteAllByclientId(existing.getId());
        // depositRepository.deleteAllByClientId(existing.getId());

        clientRepository.delete(existing);
        return "Deleted !!!";
    }

}
