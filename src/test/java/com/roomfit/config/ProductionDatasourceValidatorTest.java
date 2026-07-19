package com.roomfit.config;

import com.roomfit.RoomfitApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionDatasourceValidatorTest {

    @Test
    void acceptsPostgresqlJdbcUrlWithCredentials() {
        ProductionDatasourceValidator validator = new ProductionDatasourceValidator(
                "jdbc:postgresql://internal-host:5432/roomfit", "roomfit", "secret");

        assertThatCode(validator::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    void rejectsH2AndNonJdbcPostgresqlUrls() {
        assertThatThrownBy(() -> new ProductionDatasourceValidator(
                "jdbc:h2:file:./data/roomfit", "sa", "secret").afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jdbc:postgresql://");
        assertThatThrownBy(() -> new ProductionDatasourceValidator(
                "postgresql://host/roomfit", "roomfit", "secret").afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jdbc:postgresql://");
    }

    @Test
    void rejectsBlankProductionCredentials() {
        assertThatThrownBy(() -> new ProductionDatasourceValidator(
                "jdbc:postgresql://host/roomfit", "", "").afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SPRING_DATASOURCE_USERNAME")
                .hasMessageContaining("SPRING_DATASOURCE_PASSWORD");
    }

    @Test
    void prodApplicationContextCannotStartWithH2() {
        assertThatThrownBy(() -> new SpringApplicationBuilder(RoomfitApplication.class)
                .profiles("prod")
                .web(WebApplicationType.NONE)
                .run(
                        "--spring.main.banner-mode=off",
                        "--spring.datasource.url=jdbc:h2:mem:prod-must-not-fallback",
                        "--spring.datasource.username=sa",
                        "--spring.datasource.password=secret",
                        "--spring.jpa.hibernate.ddl-auto=none"))
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Production requires SPRING_DATASOURCE_URL in jdbc:postgresql:// format");
    }
}
