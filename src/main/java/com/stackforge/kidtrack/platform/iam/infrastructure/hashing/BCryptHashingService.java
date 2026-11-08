package com.stackforge.kidtrack.platform.iam.infrastructure.hashing;

import com.stackforge.kidtrack.platform.iam.application.internal.outboundservices.HashingService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** BCrypt implementation of the {@link HashingService} outbound port. */
@Service
public class BCryptHashingService implements HashingService {

    private final PasswordEncoder passwordEncoder;

    public BCryptHashingService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String hashedPassword) {
        return rawPassword != null && hashedPassword != null && passwordEncoder.matches(rawPassword, hashedPassword);
    }
}
