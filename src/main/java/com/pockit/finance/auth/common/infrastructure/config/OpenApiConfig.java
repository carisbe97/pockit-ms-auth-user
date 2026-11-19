package com.pockit.finance.auth.common.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${app.openapi.server-url:http://localhost:8081}")
    private String serverUrl;

    @Value("${app.openapi.server-description:Ares - Desarrollo Local (DEV)}")
    private String serverDescription;

    @Bean
    public OpenAPI pockitAuthOpenApi() {
        Server server = new Server()
                .url(serverUrl)
                .description(serverDescription);

        Contact contact = new Contact()
                .name("Camilo Aristizabal Bedoya")
                .url("https://github.com/carisbe97/pockit-ms-auth-user");

        Info info = new Info()
                .title("Pockit Finance — ms-auth-user API")
                .version("0.1.0")
                .description("Microservicio Hexagonal de Autenticacion (Spring Security + BCrypt + JWT propio), Usuarios y Avatares en Supabase Storage.")
                .contact(contact);

        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Token JWT emitido por pockit-ms-auth-user");

        return new OpenAPI()
                .info(info)
                .servers(List.of(server))
                .components(new Components().addSecuritySchemes("BearerAuth", bearerScheme));
    }
}
