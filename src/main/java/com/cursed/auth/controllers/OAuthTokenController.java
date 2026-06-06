package com.cursed.auth.controllers;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cursed.auth.dtos.TokenRequest;
import com.cursed.auth.exceptions.OAuthException;
import com.cursed.auth.services.TokenService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * OAuth token endpoint. Accepts BOTH application/json (what auth0-spa-js sends) and
 * application/x-www-form-urlencoded (older SDK versions / standard OAuth clients), and
 * returns Auth0-shaped JSON. Errors are rendered as RFC 6749 {error, error_description}.
 */
@RestController
@Tag(name = "OAuth Token", description = "Authorization-code and refresh-token grants.")
public class OAuthTokenController {

    private final TokenService tokenService;

    public OAuthTokenController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping(value = "/oauth/token", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Token endpoint (JSON body)")
    public ResponseEntity<Map<String, Object>> tokenJson(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(tokenService.token(fromJson(body)));
    }

    @PostMapping(value = "/oauth/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Token endpoint (form-encoded body)")
    public ResponseEntity<Map<String, Object>> tokenForm(@RequestParam MultiValueMap<String, String> form) {
        return ResponseEntity.ok(tokenService.token(fromForm(form)));
    }

    @ExceptionHandler(OAuthException.class)
    public ResponseEntity<Map<String, Object>> handleOAuthException(OAuthException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", ex.getError());
        body.put("error_description", ex.getErrorDescription());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    private static TokenRequest fromJson(Map<String, Object> b) {
        return new TokenRequest(
                str(b, "grant_type"),
                str(b, "client_id"),
                str(b, "client_secret"),
                str(b, "code"),
                str(b, "code_verifier"),
                str(b, "redirect_uri"),
                str(b, "refresh_token"),
                str(b, "scope"));
    }

    private static TokenRequest fromForm(MultiValueMap<String, String> f) {
        return new TokenRequest(
                f.getFirst("grant_type"),
                f.getFirst("client_id"),
                f.getFirst("client_secret"),
                f.getFirst("code"),
                f.getFirst("code_verifier"),
                f.getFirst("redirect_uri"),
                f.getFirst("refresh_token"),
                f.getFirst("scope"));
    }

    private static String str(Map<String, Object> b, String key) {
        Object v = b.get(key);
        return v == null ? null : String.valueOf(v);
    }
}
