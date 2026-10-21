package br.com.teamss.skillswap.skill_swap.model.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
    @Bean
    public DirectExchange paymentExchange(@Value("${app.messaging.rabbitmq.exchange:skillswap.payments}") String name) {
        return new DirectExchange(name, true, false);
    }

    @Bean
    public Queue paymentFulfillmentQueue(@Value("${app.messaging.rabbitmq.queue:skillswap.payments.fulfillment}") String name) {
        return new Queue(name, true);
    }

    @Bean
    public Binding paymentFulfillmentBinding(Queue paymentFulfillmentQueue, DirectExchange paymentExchange,
                                              @Value("${app.messaging.rabbitmq.routing-key:payment.fulfillment}") String key) {
        return BindingBuilder.bind(paymentFulfillmentQueue).to(paymentExchange).with(key);
    }

    @Bean
    public Jackson2JsonMessageConverter rabbitJsonConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, Jackson2JsonMessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        return factory;
    }
}
