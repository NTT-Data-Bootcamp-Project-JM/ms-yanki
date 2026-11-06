package com.nttdata.bank.yanki.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Published OpenAPI contract of the Yanki wallet service.
 */
@Configuration
public class OpenApiConfig {

    /**
     * @return contract metadata and the bearer scheme
     */
    @Bean
    public OpenAPI yankiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Yanki")
                        .version("1.0.0")
                        .description("Mobile wallet, top-up, cash-out, and transfers between phone numbers."))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
                .components(new Components().addSecuritySchemes("bearer-jwt", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
