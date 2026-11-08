package com.stackforge.kidtrack.platform.iam.infrastructure.tokens;

import com.stackforge.kidtrack.platform.iam.application.internal.outboundservices.TokenService;
import com.stackforge.kidtrack.platform.iam.domain.model.aggregates.User;
import com.stackforge.kidtrack.platform.shared.infrastructure.security.JwtProperties;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Issues HS256 signed JWT access tokens that carry the identity, role and organization of the user. */
@Service
public class JwtTokenService implements TokenService {

    private final JwtEncoder encoder;
    private final JwtProperties properties;

    public JwtTokenService(JwtEncoder encoder, JwtProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    @Override
    public String generateToken(User user) {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofDays(properties.expirationDays())))
                .claim("email", user.getEmail().value())
                .claim("name", user.getFullName().toString())
                .claim("role", user.getRole().name());
        if (user.getOrganizationId() != null) {
            claims.claim("organizationId", user.getOrganizationId().toString());
        }
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }
}
