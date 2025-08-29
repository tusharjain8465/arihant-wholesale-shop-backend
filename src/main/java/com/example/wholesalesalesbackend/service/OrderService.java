package com.example.wholesalesalesbackend.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.wholesalesalesbackend.model.Order;
import com.example.wholesalesalesbackend.repository.OrderRepository;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    public Order addOrder(Order order, Long userId, Long clientId) {

        LocalDateTime inDateIST;
        ZoneId indiaZone = ZoneId.of("Asia/Kolkata");

        if (order.getDateTime() != null) {
            ZonedDateTime zonedDateTime = order.getDateTime().atZone(indiaZone);
            inDateIST = zonedDateTime.toLocalDateTime();
        } else {
            inDateIST = LocalDateTime.now(indiaZone);
        }

        order.setDateTime(inDateIST);
        order.setClientId(clientId);
        order.setUserId(userId);

        return orderRepository.save(order);
    }

    public List<Order> getAllOrders(Long userId, Long clientId,String supplier) {
        return orderRepository.findAllByUserIdAndClientIdAndSupplierOrderByDateTimeDesc(userId,clientId,supplier);

    }

    public Order editOrder(Long id, Order updatedOrder) {
        return orderRepository.findById(id)
                .map(existing -> {
                    existing.setDateTime(updatedOrder.getDateTime());
                    existing.setSupplier(updatedOrder.getSupplier());
                    existing.setNotes(updatedOrder.getNotes());
                    return orderRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    public String deleteOrder(Long id) {
        orderRepository.deleteById(id);
        return "deleted !!!";

    }

    public Order findById(Long id) {

        return orderRepository.findById(id).get();
    }

}
