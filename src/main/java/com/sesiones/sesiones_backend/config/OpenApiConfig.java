package com.sesiones.sesiones_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sesionesOpenApi() {
        String bearerScheme = "bearerAuth";
        return new OpenAPI()
            .components(new Components().addSecuritySchemes(
                bearerScheme,
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
            ))
            .addSecurityItem(new SecurityRequirement().addList(bearerScheme))
            .info(new Info()
                .title("Sesiones Backend API")
                .version("v1")
                .description("API para la gestion y generacion de sesiones academicas"));
    }
}
