package com.stackforge.kidtrack.platform.shared.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger / OpenAPI documentation of the KidTrack Platform API. */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    OpenAPI kidTrackOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("KidTrack Platform API")
                        .version("v1")
                        .description("Backend of the KidTrack school transport web application.")
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
