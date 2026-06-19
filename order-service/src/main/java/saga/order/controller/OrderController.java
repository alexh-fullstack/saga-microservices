package saga.order.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import saga.order.domain.OrderEntity;
import saga.order.domain.OrderRepository;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderRepository orderRepository;

    @GetMapping
    public List<OrderEntity> getAllOrders() {
        return orderRepository.findAll();
    }

    @PostMapping("/reset")
    public String resetOrders() {
        orderRepository.deleteAll();
        return "Orders cleared";
    }
}
