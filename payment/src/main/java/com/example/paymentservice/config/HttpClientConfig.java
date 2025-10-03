package com.example.paymentservice.config;

import com.example.paymentservice.service.OrderServiceClient;
import com.example.paymentservice.service.impl.OrderServiceClientFallback;
import com.example.paymentservice.service.impl.OrderServiceClientImpl;
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
    public OrderServiceClientImpl orderServiceClientImpl(WebClient.Builder webClientBuilder) {
        WebClient webClient = webClientBuilder
                .baseUrl("http://order-service") // Более явное имя сервиса
                .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(WebClientAdapter.create(webClient))
                .build();

        return factory.createClient(OrderServiceClientImpl.class);
    }

    @Bean
    @Primary // Этот бин будет использоваться при автосвязывании
    public OrderServiceClient orderServiceClient(OrderServiceClientImpl orderServiceClientImpl) {
        return new OrderServiceClientFallback(orderServiceClientImpl);
    }
}