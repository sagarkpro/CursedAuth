package com.cursed.auth.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Carries an OAuth/OIDC error to be rendered as the standard {@code {error, error_description}}
 * JSON body (NOT the project's BaseResponseDTO) with an appropriate HTTP status.
 */
public class OAuthException extends RuntimeException {

    private final String error;
    private final HttpStatus status;

    public OAuthException(String error, String errorDescription, HttpStatus status) {
        super(errorDescription);
        this.error = error;
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public String getErrorDescription() {
        return getMessage();
    }

    public HttpStatus getStatus() {
        return status;
    }
}
