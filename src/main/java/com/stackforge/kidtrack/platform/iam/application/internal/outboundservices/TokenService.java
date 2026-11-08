package com.stackforge.kidtrack.platform.iam.application.internal.outboundservices;

import com.stackforge.kidtrack.platform.iam.domain.model.aggregates.User;

/** Outbound port used by the IAM application layer to issue access tokens. */
public interface TokenService {

    String generateToken(User user);
}
