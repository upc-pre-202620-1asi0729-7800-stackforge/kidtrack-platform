package com.stackforge.kidtrack.platform.shared.domain.exceptions;

/** Raised when a domain invariant or business rule is violated (HTTP 400). */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
