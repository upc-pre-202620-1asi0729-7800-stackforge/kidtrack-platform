package com.stackforge.kidtrack.platform.shared.domain.exceptions;

/** Raised when a requested aggregate does not exist (HTTP 404). */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
