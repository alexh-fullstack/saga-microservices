package saga.order.controller;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import saga.order.config.RabbitConfig;
import saga.order.domain.OrderEntity;
import saga.order.domain.OrderRepository;
import saga.order.model.SagaMessages.OrderCreatedEvent;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/checkout")
public class OrderController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @PostMapping
    public String checkout(@RequestBody CheckoutRequest request) {
        UUID orderId = UUID.randomUUID();
        UUID sagaId = UUID.randomUUID();
        System.out.println("\n[Order Service - Choreography] --- STARTING CHOREOGRAPHY TRANSACTION ---");
        System.out.println("[Order Service - Choreography] Initiating checkout for user: " + request.userId() + ", OrderID: " + orderId);

        // 1. Create order in PENDING state
        OrderEntity order = new OrderEntity(
                orderId,
                request.userId(),
                request.itemId(),
                request.quantity(),
                request.price(),
                "PENDING",
                sagaId
        );
        orderRepository.save(order);

        // 2. Publish OrderCreatedEvent
        OrderCreatedEvent event = new OrderCreatedEvent(
                sagaId,
                orderId,
                request.userId(),
                request.itemId(),
                request.quantity(),
                request.price()
        );
        System.out.println("[Order Service - Choreography] Publishing OrderCreatedEvent...");
        rabbitTemplate.convertAndSend(RabbitConfig.CHOREOGRAPHY_EXCHANGE, "order.event.created", event);

        return "Checkout Saga Initiated. ID: " + orderId;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> getDashboard() {
        Map<String, Object> data = new HashMap<>();
        
        // 1. Get orders from local DB (replaces Sagas tracker in UI)
        try {
            List<OrderEntity> orders = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
            data.put("orders", orders);
            data.put("sagas", orders); // UI maps sagas to order list
        } catch (Exception e) {
            System.err.println("Failed to fetch orders: " + e.getMessage());
            data.put("orders", Collections.emptyList());
            data.put("sagas", Collections.emptyList());
        }

        // 2. Get balances from payment-service
        try {
            Object[] balances = restTemplate.getForObject("http://payment-service:8082/payments/balances", Object[].class);
            data.put("balances", balances != null ? balances : Collections.emptyList());
        } catch (Exception e) {
            System.err.println("Failed to fetch balances from payment-service: " + e.getMessage());
            data.put("balances", Collections.emptyList());
        }

        // 3. Get inventory from stock-service
        try {
            Object[] inventory = restTemplate.getForObject("http://stock-service:8083/stocks/inventory", Object[].class);
            data.put("inventory", inventory != null ? inventory : Collections.emptyList());
        } catch (Exception e) {
            System.err.println("Failed to fetch inventory from stock-service: " + e.getMessage());
            data.put("inventory", Collections.emptyList());
        }

        return data;
    }

    @PostMapping("/reset")
    public String resetAll() {
        System.out.println("[Order Service - Choreography] Resetting all databases and states...");
        
        // 1. Clear local orders
        try {
            orderRepository.deleteAll();
        } catch (Exception e) {
            System.err.println("Failed to clear orders: " + e.getMessage());
        }

        // 2. Reset payment-service
        try {
            restTemplate.postForObject("http://payment-service:8082/payments/reset", null, String.class);
        } catch (Exception e) {
            System.err.println("Failed to reset payment-service: " + e.getMessage());
        }

        // 3. Reset stock-service
        try {
            restTemplate.postForObject("http://stock-service:8083/stocks/reset", null, String.class);
        } catch (Exception e) {
            System.err.println("Failed to reset stock-service: " + e.getMessage());
        }

        return "Database states reset successfully";
    }

    @GetMapping("/orders")
    public List<OrderEntity> getAllOrders() {
        return orderRepository.findAll();
    }

    @PostMapping("/orders/reset")
    public String resetLocalOrdersOnly() {
        orderRepository.deleteAll();
        return "Orders cleared";
    }

    public record CheckoutRequest(String userId, String itemId, int quantity, double price) {}
}
