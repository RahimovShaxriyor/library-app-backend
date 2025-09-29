package com.example.paymentservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "payment-exchange";

    public static final String PAYMENT_SUCCESS_QUEUE = "payment-success-queue";
    public static final String PAYMENT_SUCCESS_ROUTING_KEY = "payment.success";

    public static final String PAYMENT_CANCELLED_QUEUE = "payment-cancelled-queue";
    public static final String PAYMENT_CANCELLED_ROUTING_KEY = "payment.cancelled";

    public static final String PAYMENT_FAILED_QUEUE = "payment-failed-queue";
    public static final String PAYMENT_FAILED_ROUTING_KEY = "payment.failed";

    public static final String PAYMENT_CREATED_QUEUE = "payment-created-queue";
    public static final String PAYMENT_CREATED_ROUTING_KEY = "payment.created";

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue paymentSuccessQueue() {
        return new Queue(PAYMENT_SUCCESS_QUEUE, true);
    }

    @Bean
    public Queue paymentCancelledQueue() {
        return new Queue(PAYMENT_CANCELLED_QUEUE, true);
    }

    @Bean
    public Queue paymentFailedQueue() {
        return new Queue(PAYMENT_FAILED_QUEUE, true);
    }

    @Bean
    public Queue paymentCreatedQueue() {
        return new Queue(PAYMENT_CREATED_QUEUE, true);
    }

    @Bean
    public Binding paymentSuccessBinding() {
        return BindingBuilder
                .bind(paymentSuccessQueue())
                .to(paymentExchange())
                .with(PAYMENT_SUCCESS_ROUTING_KEY);
    }

    @Bean
    public Binding paymentCancelledBinding() {
        return BindingBuilder
                .bind(paymentCancelledQueue())
                .to(paymentExchange())
                .with(PAYMENT_CANCELLED_ROUTING_KEY);
    }

    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder
                .bind(paymentFailedQueue())
                .to(paymentExchange())
                .with(PAYMENT_FAILED_ROUTING_KEY);
    }

    @Bean
    public Binding paymentCreatedBinding() {
        return BindingBuilder
                .bind(paymentCreatedQueue())
                .to(paymentExchange())
                .with(PAYMENT_CREATED_ROUTING_KEY);
    }
}