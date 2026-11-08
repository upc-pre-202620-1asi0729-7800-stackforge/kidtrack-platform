package com.stackforge.kidtrack.platform.iam.application.internal.outboundservices;

/** Outbound port used by the IAM application layer to hash and verify passwords. */
public interface HashingService {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String hashedPassword);
}
