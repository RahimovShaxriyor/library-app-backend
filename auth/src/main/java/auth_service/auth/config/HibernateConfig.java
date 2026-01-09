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

            props.put(AvailableSettings.HBM2DDL_DATABASE_ACTION, "validate");

            props.put("hibernate.hbm2ddl.auto", "validate");
            props.put(AvailableSettings.DIALECT, "org.hibernate.dialect.Oracle12cDialect");

            props.put("hibernate.jdbc.time_zone", "UTC");
            props.put("hibernate.default_schema", "SYSTEM");
        };
    }
}
