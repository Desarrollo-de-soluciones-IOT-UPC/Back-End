package com.emsafe.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EMSafe API")
                        .description("""
                                Backend para la plataforma IoT de monitoreo de radiación electromagnética.

                                **Autenticación:** Usa `POST /api/auth/login` para obtener un token JWT,
                                luego haz clic en **Authorize** e ingresa el token con el formato:
                                `Bearer <tu_token>`
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("EMSafe")
                                .email("admin@emsafe.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local"),
                        new Server().url("https://emsafe-backend.azurewebsites.net").description("Producción")
                ))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME,
                                new SecurityScheme()
                                        .name(BEARER_SCHEME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Ingresa el token JWT obtenido en /api/auth/login")
                        )
                );
    }
}
