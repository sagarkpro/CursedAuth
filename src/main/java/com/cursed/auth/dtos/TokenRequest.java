package com.cursed.auth.dtos;

/**
 * Normalized /oauth/token request, built from either a JSON body or a form-encoded body
 * (auth0-spa-js sends either depending on version). Field names use OAuth's snake_case
 * keys at the wire layer; this record holds them in camelCase.
 */
public record TokenRequest(
        String grantType,
        String clientId,
        String clientSecret,
        String code,
        String codeVerifier,
        String redirectUri,
        String refreshToken,
        String scope) {
}
