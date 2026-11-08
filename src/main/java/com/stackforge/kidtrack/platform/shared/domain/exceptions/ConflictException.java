package com.stackforge.kidtrack.platform.shared.domain.exceptions;

/** Raised when an operation conflicts with the current state of the system (HTTP 409). */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
