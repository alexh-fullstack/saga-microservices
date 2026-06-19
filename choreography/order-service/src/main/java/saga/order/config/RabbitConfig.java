package saga.order.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import saga.order.model.SagaMessages.*;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitConfig {

    public static final String CHOREOGRAPHY_EXCHANGE = "choreography-exchange";
    public static final String ORDER_EVENTS_QUEUE = "order-events-queue";

    @Bean
    public TopicExchange choreographyExchange() {
        return new TopicExchange(CHOREOGRAPHY_EXCHANGE);
    }

    @Bean
    public Queue orderEventsQueue() {
        return new Queue(ORDER_EVENTS_QUEUE);
    }

    @Bean
    public Binding paymentEventsBinding(Queue orderEventsQueue, TopicExchange choreographyExchange) {
        return BindingBuilder.bind(orderEventsQueue).to(choreographyExchange).with("payment.event.#");
    }

    @Bean
    public Binding stockEventsBinding(Queue orderEventsQueue, TopicExchange choreographyExchange) {
        return BindingBuilder.bind(orderEventsQueue).to(choreographyExchange).with("stock.event.#");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        
        Map<String, Class<?>> idClassMapping = new HashMap<>();
        idClassMapping.put("OrderCreatedEvent", OrderCreatedEvent.class);
        idClassMapping.put("PaymentResultEvent", PaymentResultEvent.class);
        idClassMapping.put("StockResultEvent", StockResultEvent.class);
        
        typeMapper.setIdClassMapping(idClassMapping);
        typeMapper.setTrustedPackages("*");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
