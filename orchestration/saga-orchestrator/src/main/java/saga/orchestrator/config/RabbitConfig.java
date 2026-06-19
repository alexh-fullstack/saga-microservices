package saga.orchestrator.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import saga.orchestrator.model.SagaMessages.*;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitConfig {

    public static final String SAGA_EXCHANGE = "saga-exchange";
    
    public static final String ORCHESTRATOR_QUEUE = "orchestrator-queue";
    public static final String ORDER_COMMANDS_QUEUE = "order-commands-queue";
    public static final String PAYMENT_COMMANDS_QUEUE = "payment-commands-queue";
    public static final String STOCK_COMMANDS_QUEUE = "stock-commands-queue";

    public static final String SAGA_EVENT_ROUTING_KEY = "saga.event.#";
    public static final String ORDER_COMMAND_ROUTING_KEY = "order.command.*";
    public static final String PAYMENT_COMMAND_ROUTING_KEY = "payment.command.*";
    public static final String STOCK_COMMAND_ROUTING_KEY = "stock.command.*";

    @Bean
    public TopicExchange sagaExchange() {
        return new TopicExchange(SAGA_EXCHANGE);
    }

    @Bean
    public Queue orchestratorQueue() {
        return new Queue(ORCHESTRATOR_QUEUE);
    }

    @Bean
    public Queue orderCommandsQueue() {
        return new Queue(ORDER_COMMANDS_QUEUE);
    }

    @Bean
    public Queue paymentCommandsQueue() {
        return new Queue(PAYMENT_COMMANDS_QUEUE);
    }

    @Bean
    public Queue stockCommandsQueue() {
        return new Queue(STOCK_COMMANDS_QUEUE);
    }

    @Bean
    public Binding orchestratorBinding(Queue orchestratorQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(orchestratorQueue).to(sagaExchange).with(SAGA_EVENT_ROUTING_KEY);
    }

    @Bean
    public Binding orderCommandsBinding(Queue orderCommandsQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(orderCommandsQueue).to(sagaExchange).with(ORDER_COMMAND_ROUTING_KEY);
    }

    @Bean
    public Binding paymentCommandsBinding(Queue paymentCommandsQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(paymentCommandsQueue).to(sagaExchange).with(PAYMENT_COMMAND_ROUTING_KEY);
    }

    @Bean
    public Binding stockCommandsBinding(Queue stockCommandsQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(stockCommandsQueue).to(sagaExchange).with(STOCK_COMMAND_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        
        Map<String, Class<?>> idClassMapping = new HashMap<>();
        idClassMapping.put("CreateOrderCommand", CreateOrderCommand.class);
        idClassMapping.put("ConfirmOrderCommand", ConfirmOrderCommand.class);
        idClassMapping.put("CancelOrderCommand", CancelOrderCommand.class);
        idClassMapping.put("OrderCreatedEvent", OrderCreatedEvent.class);
        idClassMapping.put("ReservePaymentCommand", ReservePaymentCommand.class);
        idClassMapping.put("RefundPaymentCommand", RefundPaymentCommand.class);
        idClassMapping.put("PaymentResultEvent", PaymentResultEvent.class);
        idClassMapping.put("ReserveStockCommand", ReserveStockCommand.class);
        idClassMapping.put("ReleaseStockCommand", ReleaseStockCommand.class);
        idClassMapping.put("StockResultEvent", StockResultEvent.class);
        
        typeMapper.setIdClassMapping(idClassMapping);
        typeMapper.setTrustedPackages("*");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
