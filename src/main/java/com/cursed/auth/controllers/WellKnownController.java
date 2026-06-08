package com.cursed.auth.controllers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * OIDC discovery + JWKS. The issuer is published WITH a trailing slash to match what the
 * Auth0 SDK derives from the configured domain ({@code https://{domain}/}); endpoint URLs
 * are built off the slash-stripped base to avoid a double slash.
 */
@RestController
@Tag(name = "OIDC Discovery", description = "OpenID Connect discovery document and JWKS.")
public class WellKnownController {

    private final JWKSource<SecurityContext> jwkSource;
    private final String issuer;
    private final String base;

    public WellKnownController(JWKSource<SecurityContext> jwkSource, @Value("${sso.issuer}") String issuer) {
        this.jwkSource = jwkSource;
        this.issuer = issuer;
        this.base = issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) : issuer;
    }

    @GetMapping(value = "/.well-known/openid-configuration", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "OpenID Connect discovery document")
    public Map<String, Object> openidConfiguration() {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("issuer", issuer);
        doc.put("authorization_endpoint", base + "/authorize");
        doc.put("token_endpoint", base + "/oauth/token");
        doc.put("jwks_uri", base + "/.well-known/jwks.json");
        doc.put("userinfo_endpoint", base + "/userinfo");
        doc.put("end_session_endpoint", base + "/v2/logout");
        doc.put("response_types_supported", List.of("code"));
        doc.put("response_modes_supported", List.of("query"));
        doc.put("grant_types_supported", List.of("authorization_code", "refresh_token"));
        doc.put("subject_types_supported", List.of("public"));
        doc.put("id_token_signing_alg_values_supported", List.of("RS256"));
        doc.put("scopes_supported", List.of("openid", "profile", "email", "offline_access"));
        doc.put("token_endpoint_auth_methods_supported", List.of("none", "client_secret_post"));
        doc.put("code_challenge_methods_supported", List.of("S256"));
        doc.put("claims_supported", List.of(
                "sub", "iss", "aud", "exp", "iat", "auth_time", "nonce", "azp",
                "name", "given_name", "family_name", "middle_name", "picture", "updated_at",
                "email", "email_verified"));
        return doc;
    }

    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "JSON Web Key Set (public signing keys)")
    public Map<String, Object> jwks() throws KeySourceException {
        List<JWK> keys = jwkSource.get(new JWKSelector(new JWKMatcher.Builder().build()), null);
        return new JWKSet(keys).toPublicJWKSet().toJSONObject(true);
    }
}
