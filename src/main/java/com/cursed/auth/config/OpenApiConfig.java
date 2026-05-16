package com.cursed.auth.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {
    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI cursedAuthOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cursed Auth API")
                        .version("0.0.1")
                        .description("JWT authentication, email OTP verification, and RBAC-ready user identity."))
                .servers(List.of(
                        new Server()
                                .url("https://api.auth.cursedshrine.co.in")
                                .description("Production"),
                        new Server()
                                .url("http://localhost:7772")
                                .description("Local development")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
