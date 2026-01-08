package auth_service.auth.config;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HibernateConfig {

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return props -> {
            // Включите проверку схемы (для IntelliJ)
            props.put(AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, "validate");

            // Отключите генерацию DDL через Hibernate
            props.put(AvailableSettings.HBM2DDL_AUTO, "validate");
            props.put(AvailableSettings.GENERATE_DDL, false);

            props.put(AvailableSettings.DIALECT, "org.hibernate.dialect.Oracle12cDialect");

            // Настройки для правильной работы с Oracle
            props.put(AvailableSettings.JPA_JDBC_TIME_ZONE, "UTC");
            props.put("hibernate.default_schema", "SYSTEM"); // или ваш пользователь
        };
    }
}