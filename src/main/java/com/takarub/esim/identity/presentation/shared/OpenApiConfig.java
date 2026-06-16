package com.takarub.esim.identity.presentation.shared;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI identityOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Takarub eSIM Identity API")
                        .description("Identity module REST API (v1)")
                        .version("v1"));
    }
}
