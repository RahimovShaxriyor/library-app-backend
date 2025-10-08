package order_service.order.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "payment-exchange";

    public static final String PAYMENT_SUCCESS_QUEUE = "payment-success-queue";
    public static final String PAYMENT_CANCELLED_QUEUE = "payment-cancelled-queue";

    public static final String DLQ_NAME = "payment-dlq";
    public static final String DLX_NAME = "payment-dlx";

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue paymentSuccessQueue() {
        return QueueBuilder.durable(PAYMENT_SUCCESS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", "deadLetter")
                .build();
    }

    @Bean
    public Queue paymentCancelledQueue() {
        return QueueBuilder.durable(PAYMENT_CANCELLED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", "deadLetter")
                .build();
    }

    @Bean
    public FanoutExchange deadLetterExchange() {
        return new FanoutExchange(DLX_NAME);
    }

    @Bean
    public Queue deadLetterQueue() {
        return new Queue(DLQ_NAME, true);
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange());
    }
}
