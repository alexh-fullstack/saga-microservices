package saga.stock.config;

import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import saga.stock.model.SagaMessages.*;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitConfig {
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        
        Map<String, Class<?>> idClassMapping = new HashMap<>();
        idClassMapping.put("ReserveStockCommand", ReserveStockCommand.class);
        idClassMapping.put("ReleaseStockCommand", ReleaseStockCommand.class);
        idClassMapping.put("StockResultEvent", StockResultEvent.class);
        
        typeMapper.setIdClassMapping(idClassMapping);
        typeMapper.setTrustedPackages("*");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
