package com.stackforge.kidtrack.platform.shared.domain.exceptions;

/** Raised when the credentials supplied to sign in are not valid (HTTP 401). */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
