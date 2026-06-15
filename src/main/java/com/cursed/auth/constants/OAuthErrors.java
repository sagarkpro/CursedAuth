package com.cursed.auth.constants;

/**
 * RFC 6749 / Auth0 OAuth error codes used in the {error, error_description}
 * JSON bodies.
 */
public final class OAuthErrors {

    private OAuthErrors() {
        super();
    }

    public static final String INVALID_REQUEST = "invalid_request";
    public static final String INVALID_CLIENT = "invalid_client";
    public static final String INVALID_GRANT = "invalid_grant";
    public static final String UNAUTHORIZED_CLIENT = "unauthorized_client";
    public static final String UNSUPPORTED_GRANT_TYPE = "unsupported_grant_type";
    public static final String INVALID_SCOPE = "invalid_scope";
    public static final String ACCESS_DENIED = "access_denied";
    public static final String SERVER_ERROR = "server_error";
    public static final String UNSUPPORTED_RESPONSE_TYPE = "unsupported_response_type";
}
