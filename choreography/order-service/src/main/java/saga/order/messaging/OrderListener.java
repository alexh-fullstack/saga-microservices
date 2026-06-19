package saga.order.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import saga.order.config.RabbitConfig;
import saga.order.domain.OrderRepository;
import saga.order.model.SagaMessages.PaymentResultEvent;
import saga.order.model.SagaMessages.StockResultEvent;

@Component
@RabbitListener(queues = RabbitConfig.ORDER_EVENTS_QUEUE)
public class OrderListener {

    @Autowired
    private OrderRepository orderRepository;

    @RabbitHandler
    public void handlePaymentResult(PaymentResultEvent event) {
        System.out.println("[Order Service] Received PaymentResultEvent for Saga: " + event.sagaId() + ", Success: " + event.success());
        orderRepository.findById(event.orderId()).ifPresent(order -> {
            if (event.success()) {
                order.setPaymentStatus("SUCCESS");
            } else {
                order.setPaymentStatus("FAILED");
                order.setStatus("CANCELLED");
                order.setErrorMessage(event.message());
            }
            orderRepository.save(order);
        });
    }

    @RabbitHandler
    public void handleStockResult(StockResultEvent event) {
        System.out.println("[Order Service] Received StockResultEvent for Saga: " + event.sagaId() + ", Success: " + event.success());
        orderRepository.findById(event.orderId()).ifPresent(order -> {
            if (event.success()) {
                order.setStockStatus("SUCCESS");
                order.setStatus("CONFIRMED");
            } else {
                order.setStockStatus("FAILED");
                order.setPaymentStatus("REFUNDED");
                order.setStatus("CANCELLED");
                order.setErrorMessage(event.message());
            }
            orderRepository.save(order);
        });
    }
}
