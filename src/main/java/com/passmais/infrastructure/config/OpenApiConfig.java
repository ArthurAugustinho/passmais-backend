package com.passmais.infrastructure.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI passmaisOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Passmais API")
                        .description("API de agendamento de consultas médicas - Passmais")
                        .version("v1"))
                .externalDocs(new ExternalDocumentation()
                        .description("Documentação OpenAPI")
                        .url("/swagger-ui.html"));
    }
}

