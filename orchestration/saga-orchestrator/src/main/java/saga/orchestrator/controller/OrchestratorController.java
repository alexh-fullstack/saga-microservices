package saga.orchestrator.controller;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import saga.orchestrator.domain.SagaStateEntity;
import saga.orchestrator.domain.SagaStateRepository;
import saga.orchestrator.model.SagaMessages.CreateOrderCommand;
import saga.orchestrator.config.RabbitConfig;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

@RestController
@RequestMapping("/checkout")
public class OrchestratorController {

    @Autowired
    private SagaStateRepository sagaRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @PostMapping
    public String checkout(@RequestBody CheckoutRequest request) {
        UUID sagaId = UUID.randomUUID();
        System.out.println("\n[Orchestrator] --- STARTING SAGA SESSIONS ---");
        System.out.println("[Orchestrator] Initiating checkout saga: " + sagaId + " for user: " + request.userId());

        // 1. Save Saga State in DB
        SagaStateEntity saga = new SagaStateEntity(
                sagaId,
                request.userId(),
                request.itemId(),
                request.quantity(),
                request.price(),
                "STARTED"
        );
        sagaRepository.save(saga);

        // 2. Send CreateOrderCommand
        CreateOrderCommand cmd = new CreateOrderCommand(
                sagaId,
                request.userId(),
                request.itemId(),
                request.quantity(),
                request.price()
        );
        System.out.println("[Orchestrator] Sending CreateOrderCommand via RabbitMQ...");
        rabbitTemplate.convertAndSend(RabbitConfig.SAGA_EXCHANGE, "order.command.create", cmd);

        return "Checkout Saga Initiated. ID: " + sagaId;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> getDashboard() {
        Map<String, Object> data = new HashMap<>();
        
        // 1. Get all sagas sorted by createdAt descending
        try {
            data.put("sagas", sagaRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")));
        } catch (Exception e) {
            System.err.println("Failed to fetch sagas: " + e.getMessage());
            data.put("sagas", Collections.emptyList());
        }

        // 2. Get orders from order-service
        try {
            Object[] orders = restTemplate.getForObject("http://order-service:8081/orders", Object[].class);
            data.put("orders", orders != null ? orders : Collections.emptyList());
        } catch (Exception e) {
            System.err.println("Failed to fetch orders from order-service: " + e.getMessage());
            data.put("orders", Collections.emptyList());
        }

        // 3. Get balances from payment-service
        try {
            Object[] balances = restTemplate.getForObject("http://payment-service:8082/payments/balances", Object[].class);
            data.put("balances", balances != null ? balances : Collections.emptyList());
        } catch (Exception e) {
            System.err.println("Failed to fetch balances from payment-service: " + e.getMessage());
            data.put("balances", Collections.emptyList());
        }

        // 4. Get inventory from stock-service
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
        System.out.println("[Orchestrator] Resetting all databases and states...");
        
        // 1. Clear sagas
        try {
            sagaRepository.deleteAll();
        } catch (Exception e) {
            System.err.println("Failed to clear sagas: " + e.getMessage());
        }

        // 2. Reset order-service
        try {
            restTemplate.postForObject("http://order-service:8081/orders/reset", null, String.class);
        } catch (Exception e) {
            System.err.println("Failed to reset order-service: " + e.getMessage());
        }

        // 3. Reset payment-service
        try {
            restTemplate.postForObject("http://payment-service:8082/payments/reset", null, String.class);
        } catch (Exception e) {
            System.err.println("Failed to reset payment-service: " + e.getMessage());
        }

        // 4. Reset stock-service
        try {
            restTemplate.postForObject("http://stock-service:8083/stocks/reset", null, String.class);
        } catch (Exception e) {
            System.err.println("Failed to reset stock-service: " + e.getMessage());
        }

        return "Database states reset successfully";
    }

    public record CheckoutRequest(String userId, String itemId, int quantity, double price) {}
}
