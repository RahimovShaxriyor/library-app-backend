package auth_service.auth.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Отключаем CSRF-защиту. Это главная причина ошибки 403 Forbidden.
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Настраиваем правила авторизации.
                .authorizeHttpRequests(req ->
                        req.requestMatchers(
                                        // УЛУЧШЕНИЕ: Явно указываем ТОЛЬКО эндпоинты, которые должны быть публичными.
                                        // Это более безопасно, чем разрешать все подряд по маске "/api/auth/**".
                                        "/api/auth/register", // Регистрация
                                        "/api/auth/login",    // Вход в систему

                                        // Документация API также остается публичной.
                                        "/swagger-ui/**",
                                        "/v3/api-docs/**"
                                )
                                .permitAll()              // Разрешаем доступ к ним для всех.
                                .anyRequest()             // Все остальные запросы...
                                .authenticated()          // ...требуют аутентификации.
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
