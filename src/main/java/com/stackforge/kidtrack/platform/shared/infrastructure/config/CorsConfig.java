package com.stackforge.kidtrack.platform.shared.infrastructure.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS policy. When {@code kidtrack.cors.allowed-origins} is empty any origin is accepted, which is
 * convenient for development; production deployments should list the web application origin.
 */
@Configuration
public class CorsConfig {

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${kidtrack.cors.allowed-origins:}") List<String> allowedOrigins) {
        var configuration = new CorsConfiguration();
        var origins = allowedOrigins.stream().filter(origin -> !origin.isBlank()).toList();
        if (origins.isEmpty()) {
            configuration.addAllowedOriginPattern("*");
        } else {
            configuration.setAllowedOrigins(origins);
        }
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
