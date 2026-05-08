package com.rentalcar.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title       = "Rental Car Booking API",
        version     = "1.0",
        description = "REST API for car rental — authentication, search, booking lifecycle, admin fleet management",
        contact     = @Contact(name = "Rental Car Team", email = "dev@rentalcar.com")
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Local development"),
        @Server(url = "https://api.rentalcar.com", description = "Production")
    }
)
@SecurityScheme(
    name         = "bearerAuth",
    type         = SecuritySchemeType.HTTP,
    scheme       = "bearer",
    bearerFormat = "JWT",
    in           = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {

}
