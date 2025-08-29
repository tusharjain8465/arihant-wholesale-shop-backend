// package com.example.wholesalesalesbackend.controllers;

// import java.util.ArrayList;
// import java.util.List;
// import java.util.Optional;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.DeleteMapping;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RequestParam;
// import org.springframework.web.bind.annotation.RestController;

// import com.example.wholesalesalesbackend.dto.UserClientFeatureDTO;
// import com.example.wholesalesalesbackend.model.Client;
// import com.example.wholesalesalesbackend.model.Feature;
// import com.example.wholesalesalesbackend.model.User;
// import com.example.wholesalesalesbackend.model.UserClientFeature;
// import com.example.wholesalesalesbackend.repository.ClientRepository;
// import com.example.wholesalesalesbackend.repository.FeatureRepository;
// import com.example.wholesalesalesbackend.repository.UserClientRepository;
// import com.example.wholesalesalesbackend.repository.UserRepository;

// @RestController
// @RequestMapping("/api/feature")
// public class UserFeatureController {

//     @Autowired(required = false)
//     private UserClientRepository userClientRepository;

//     @Autowired(required = false)
//     private FeatureRepository featureRepository;

//     @Autowired(required = false)
//     private UserRepository userRepository;

//     @Autowired(required = false)
//     private ClientRepository clientRepository;

//     @GetMapping("/fetch/shop/staff")
//     public ResponseEntity<List<UserClientFeatureDTO>> fetchStaffToFeatureOfShop(@RequestParam String clientName,
//             @RequestParam String featureName,
//             @RequestParam Long userId) {

//         Optional<Feature> feature = featureRepository.findByName(featureName);
//         Optional<Client> client = clientRepository.findByName(clientName);

//         List<UserClientFeature> userClientFeatures = userClientRepository.findAllByOwnerIdAndClientIdAndFeatureId(
//                 userId,
//                 client.get().getId(), feature.get().getId());

//         List<UserClientFeatureDTO> dtos = new ArrayList<>();

//         for (UserClientFeature userClient : userClientFeatures) {

//             UserClientFeatureDTO dto = new UserClientFeatureDTO();
//             String ownerName = userRepository.findUsernameById(userClient.getOwnerId());

//             String userName = userRepository.findUsernameById(userClient.getUserId());

//             dto.setClientName(clientRepository.findById(userId).get().getName());
//             dto.setFeatureName(featureName);
//             dto.setOwnerName(ownerName);
//             dto.setStaffName(userName);
//             dto.setId(userClient.getId());
//             dtos.add(dto);
//         }

//         return ResponseEntity.ok(dtos);
//     }

//     @GetMapping("all/fetch/shop/staff")
//     public ResponseEntity<List<UserClientFeatureDTO>> fetchStaffToAllFeatureOfShop(@RequestParam String clientName,
//             @RequestParam Long userId) {

//         Optional<Client> client = clientRepository.findByName(clientName);

//         List<UserClientFeature> userClientFeatures = userClientRepository.findAllByOwnerIdAndClientId(
//                 userId,
//                 client.get().getId());

//         List<UserClientFeatureDTO> dtos = new ArrayList<>();

//         for (UserClientFeature userClient : userClientFeatures) {

//             UserClientFeatureDTO dto = new UserClientFeatureDTO();
//             String ownerName = userRepository.findUsernameById(userClient.getOwnerId());
//             String featureName = featureRepository.findById(userClient.getFeatureId()).get().getName();
//             String userName = userRepository.findUsernameById(userClient.getUserId());

//             dto.setClientName(clientRepository.findById(userClient.getClientId()).get().getName());
//             dto.setFeatureName(featureName);
//             dto.setOwnerName(ownerName);
//             dto.setStaffName(userName);
//             dto.setId(userClient.getId());
//             dtos.add(dto);
//         }

//         return ResponseEntity.ok(dtos);
//     }

//     @GetMapping("/fetch")
//     public ResponseEntity<List<Feature>> fetchFeatures() {


//         return ResponseEntity.ok(featureRepository.findAll());
//     }

//     @DeleteMapping("/delete/shop/staff")
//     public ResponseEntity<String> deleteStaffToFeatureOfShop(@RequestParam String clientName,
//             @RequestParam String featureName,
//             @RequestParam String staffName,
//             @RequestParam Long userId) {

//         Optional<Feature> feature = featureRepository.findByName(featureName);
//         Optional<User> staff = userRepository.findByUsername(staffName);
//         Optional<Client> client = clientRepository.findByName(clientName);

//         Optional<UserClientFeature> userClientFeature = userClientRepository
//                 .findByOwnerIdAndFeatureIdAndClientIdAndUserId(userId, feature.get().getId(), client.get().getId(),
//                         staff.get().getId());

//         userClientRepository.delete(userClientFeature.get());

//         return ResponseEntity.ok("Feature deleted for staff");

//     }

// }