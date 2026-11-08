package com.stackforge.kidtrack.platform.shared.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings of the JWT tokens issued by the IAM bounded context.
 *
 * @param secret         HMAC secret used to sign the tokens (at least 32 characters)
 * @param expirationDays number of days a token remains valid
 */
@ConfigurationProperties(prefix = "kidtrack.security.jwt")
public record JwtProperties(String secret, int expirationDays) {

    public JwtProperties {
        if (expirationDays <= 0) {
            expirationDays = 7;
        }
    }
}
