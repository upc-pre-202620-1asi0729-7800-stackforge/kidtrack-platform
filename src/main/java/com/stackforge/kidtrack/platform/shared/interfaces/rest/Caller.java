package com.stackforge.kidtrack.platform.shared.interfaces.rest;

import java.util.Arrays;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * The authenticated caller of a request, read from the claims of the JWT issued by the IAM context.
 *
 * @param userId         identifier of the user (JWT subject)
 * @param role           role tier of the user (ADMIN, DRIVER or PARENT)
 * @param organizationId organization the user belongs to, or {@code null} when not assigned yet
 */
public record Caller(UUID userId, String role, UUID organizationId) {

    public static Caller from(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        var organization = jwt.getClaimAsString("organizationId");
        return new Caller(
                UUID.fromString(jwt.getSubject()),
                jwt.getClaimAsString("role"),
                organization == null || organization.isBlank() ? null : UUID.fromString(organization));
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    /**
     * Resolves the organization a query must be scoped to. When the request names an organization it must
     * be the caller's own; when it does not, the caller's organization is used.
     */
    public static UUID scope(Caller caller, UUID requested) {
        if (caller == null || caller.organizationId() == null) {
            return requested;
        }
        if (requested != null && !requested.equals(caller.organizationId())) {
            throw new AccessDeniedException("The resource belongs to another organization.");
        }
        return caller.organizationId();
    }

    /** Same as {@link #scope(Caller, UUID)} but the organization is mandatory. */
    public static UUID requireScope(Caller caller, UUID requested) {
        var scope = scope(caller, requested);
        if (scope == null) {
            throw new IllegalArgumentException("organizationId is required.");
        }
        return scope;
    }

    /** Fails when the caller is not an administrator. */
    public static void requireAdmin(Caller caller) {
        requireAnyRole(caller, "ADMIN");
    }

    /** Fails when the caller does not have one of the given roles. */
    public static void requireAnyRole(Caller caller, String... roles) {
        if (caller == null || Arrays.stream(roles).noneMatch(role -> role.equals(caller.role()))) {
            throw new AccessDeniedException("You are not allowed to perform this operation.");
        }
    }
}
