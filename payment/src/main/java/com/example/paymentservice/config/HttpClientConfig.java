package com.example.paymentservice.config;

import com.example.paymentservice.service.OrderServiceClient;
import com.example.paymentservice.service.impl.OrderServiceClientImpl;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class HttpClientConfig {

    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public HttpClient reactiveHttpClient() {
        return HttpClient.create()
                .responseTimeout(Duration.ofSeconds(30))
                .compress(true);
    }

    @Bean
    public OrderServiceClient orderServiceClient(WebClient.Builder webClientBuilder, HttpClient httpClient) {
        ReactorClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);

        WebClient webClient = webClientBuilder
                .baseUrl("http://order-service")
                .clientConnector(connector)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-Service-Name", "payment-service")
                .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builder(WebClientAdapter.forClient(webClient))
                .blockTimeout(Duration.ofSeconds(30))
                .build();

        // Создаем реализацию интерфейса через HttpServiceProxyFactory
        return factory.createClient(OrderServiceClient.class);
    }

    // Дополнительный бин для локальной разработки
    @Bean
    public WebClient simpleWebClient() {
        return WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}