package saga.payment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import saga.payment.model.SagaMessages.*;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitConfig {

    public static final String CHOREOGRAPHY_EXCHANGE = "choreography-exchange";
    public static final String PAYMENT_EVENTS_QUEUE = "payment-events-queue";

    @Bean
    public TopicExchange choreographyExchange() {
        return new TopicExchange(CHOREOGRAPHY_EXCHANGE);
    }

    @Bean
    public Queue paymentEventsQueue() {
        return new Queue(PAYMENT_EVENTS_QUEUE);
    }

    @Bean
    public Binding orderCreatedBinding(Queue paymentEventsQueue, TopicExchange choreographyExchange) {
        return BindingBuilder.bind(paymentEventsQueue).to(choreographyExchange).with("order.event.created");
    }

    @Bean
    public Binding stockFailedBinding(Queue paymentEventsQueue, TopicExchange choreographyExchange) {
        return BindingBuilder.bind(paymentEventsQueue).to(choreographyExchange).with("stock.event.failed");
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
