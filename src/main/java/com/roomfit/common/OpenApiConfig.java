package com.roomfit.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI roomFitOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RoomFit-Backend")
                        .version("0.0.1")
                        .description("RoomFit AI MVP backend API"));
    }
}
