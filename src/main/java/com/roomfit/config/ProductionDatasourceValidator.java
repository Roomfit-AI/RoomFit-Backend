package com.roomfit.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ProductionDatasourceValidator implements InitializingBean {

    private final String url;
    private final String username;
    private final String password;

    public ProductionDatasourceValidator(@Value("${spring.datasource.url}") String url,
                                         @Value("${spring.datasource.username}") String username,
                                         @Value("${spring.datasource.password}") String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    @Override
    public void afterPropertiesSet() {
        if (url == null || !url.startsWith("jdbc:postgresql://")) {
            throw new IllegalStateException(
                    "Production requires SPRING_DATASOURCE_URL in jdbc:postgresql:// format");
        }
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Production requires SPRING_DATASOURCE_USERNAME and SPRING_DATASOURCE_PASSWORD");
        }
    }
}
