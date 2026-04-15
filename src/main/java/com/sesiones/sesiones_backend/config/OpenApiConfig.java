package com.sesiones.sesiones_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sesionesOpenApi() {
        return new OpenAPI().info(new Info()
            .title("Sesiones Backend API")
            .version("v1")
            .description("API para la gestion y generacion de sesiones academicas"));
    }
}
