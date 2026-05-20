package com.notfound.orderservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_CANCELLED_QUEUE = "order.cancelled.queue";
    public static final String ORDER_CANCELLED_KEY = "order.cancelled";

    // Saga command/event exchanges
    public static final String COMMAND_EXCHANGE = "bookstore.commands";
    public static final String EVENT_EXCHANGE = "bookstore.events";
    public static final String ORDER_COMMANDS_QUEUE = "order.commands.queue";
    public static final String ORDER_CREATE_COMMAND_KEY = "order.create.command";
    public static final String ORDER_CONFIRM_COMMAND_KEY = "order.confirm.command";
    public static final String ORDER_CANCEL_COMMAND_KEY = "order.cancel.command";
    public static final String ORDER_CREATED_KEY = "order.created";
    public static final String ORDER_CONFIRMED_KEY = "order.confirmed";
    public static final String ORDER_FAILED_KEY = "order.failed";

    // Payment exchange + queue (khai báo để consume)
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public TopicExchange commandExchange() {
        return new TopicExchange(COMMAND_EXCHANGE);
    }

    @Bean
    public TopicExchange eventExchange() {
        return new TopicExchange(EVENT_EXCHANGE);
    }

    @Bean
    public Queue orderCommandsQueue() {
        return new Queue(ORDER_COMMANDS_QUEUE, true);
    }

    @Bean
    public Binding orderCreateCommandBinding(Queue orderCommandsQueue, TopicExchange commandExchange) {
        return BindingBuilder.bind(orderCommandsQueue).to(commandExchange).with(ORDER_CREATE_COMMAND_KEY);
    }

    @Bean
    public Binding orderConfirmCommandBinding(Queue orderCommandsQueue, TopicExchange commandExchange) {
        return BindingBuilder.bind(orderCommandsQueue).to(commandExchange).with(ORDER_CONFIRM_COMMAND_KEY);
    }

    @Bean
    public Binding orderCancelCommandBinding(Queue orderCommandsQueue, TopicExchange commandExchange) {
        return BindingBuilder.bind(orderCommandsQueue).to(commandExchange).with(ORDER_CANCEL_COMMAND_KEY);
    }

    @Bean
    public Queue orderCancelledQueue() {
        return new Queue(ORDER_CANCELLED_QUEUE, true);
    }

    @Bean
    public Binding orderCancelledBinding(Queue orderCancelledQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderCancelledQueue).to(orderExchange).with(ORDER_CANCELLED_KEY);
    }

    @Bean
    public MessageConverter jacksonJsonMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        converter.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jacksonJsonMessageConverter());
        return template;
    }

}
