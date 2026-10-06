package com.utkarsh.ticketbooking.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Swagger setup. Adds the "Authorize" button so a JWT can be pasted in Swagger UI.
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ticketBookingOpenApi() {
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        Info info = new Info()
                .title("Event Ticket Booking API")
                .version("1.0.0")
                .description("Organisers publish events with limited seats, attendees book or cancel them.");

        return new OpenAPI()
                .info(info)
                .components(new Components().addSecuritySchemes("bearerAuth", bearerScheme))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
