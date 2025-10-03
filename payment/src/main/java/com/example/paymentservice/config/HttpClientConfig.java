package com.example.paymentservice.config;

import com.example.paymentservice.service.OrderServiceClient;
import com.example.paymentservice.service.impl.OrderServiceClientFallback;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class HttpClientConfig {

    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public OrderServiceClient orderServiceClient(WebClient.Builder webClientBuilder) {
        WebClient webClient = webClientBuilder
                .baseUrl("http://order-service")
                .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(WebClientAdapter.create(webClient))
                .build();

        // Создаем клиент напрямую из интерфейса OrderServiceClient
        return factory.createClient(OrderServiceClient.class);
    }

    @Bean
    @Primary
    public OrderServiceClient orderServiceClientWithFallback(OrderServiceClient orderServiceClient) {
        return new OrderServiceClientFallback(orderServiceClient);
    }
}