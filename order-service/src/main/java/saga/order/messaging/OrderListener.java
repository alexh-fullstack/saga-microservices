package saga.order.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import saga.order.domain.OrderEntity;
import saga.order.domain.OrderRepository;
import saga.order.model.SagaMessages.*;

import java.util.UUID;

@Component
@RabbitListener(queues = "order-commands-queue")
public class OrderListener {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @RabbitHandler
    public void handleCreateOrder(CreateOrderCommand cmd) {
        UUID orderId = UUID.randomUUID();
        System.out.println("[Order Service] Received CreateOrderCommand for Saga: " + cmd.sagaId());

        try {
            OrderEntity order = new OrderEntity(
                    orderId,
                    cmd.userId(),
                    cmd.itemId(),
                    cmd.quantity(),
                    cmd.price(),
                    "PENDING",
                    cmd.sagaId()
            );
            orderRepository.save(order);

            System.out.println("[Order Service] PENDING order created: " + orderId);
            
            OrderCreatedEvent response = new OrderCreatedEvent(cmd.sagaId(), orderId, true, "Order created successfully");
            rabbitTemplate.convertAndSend("saga-exchange", "saga.event.order", response);
        } catch (Exception e) {
            System.err.println("[Order Service] Failed to create order: " + e.getMessage());
            OrderCreatedEvent response = new OrderCreatedEvent(cmd.sagaId(), null, false, e.getMessage());
            rabbitTemplate.convertAndSend("saga-exchange", "saga.event.order", response);
        }
    }

    @RabbitHandler
    public void handleConfirmOrder(ConfirmOrderCommand cmd) {
        System.out.println("[Order Service] Received ConfirmOrderCommand for order: " + cmd.orderId());
        orderRepository.findById(cmd.orderId()).ifPresent(order -> {
            order.setStatus("CONFIRMED");
            orderRepository.save(order);
            System.out.println("[Order Service] Order CONFIRMED: " + order.getId());
        });
    }

    @RabbitHandler
    public void handleCancelOrder(CancelOrderCommand cmd) {
        System.out.println("[Order Service] Received CancelOrderCommand (Compensation) for order: " + cmd.orderId());
        orderRepository.findById(cmd.orderId()).ifPresent(order -> {
            order.setStatus("CANCELLED");
            orderRepository.save(order);
            System.out.println("[Order Service] Order CANCELLED: " + order.getId());
        });
    }
}
