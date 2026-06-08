package com.cursed.auth.controllers;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cursed.auth.constants.OAuthErrors;
import com.cursed.auth.entities.OAuthClient;
import com.cursed.auth.repository.OAuthClientRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Auth0-style logout. The SDK navigates the browser to /v2/logout?client_id=&returnTo=.
 * We are stateless (no SSO session to clear in v1), so this validates returnTo against the
 * client's registered post-logout URIs (to prevent open redirects) and 302s there.
 */
@RestController
@Tag(name = "Logout", description = "Auth0-style logout redirect.")
public class LogoutController {

    private final OAuthClientRepository oAuthClientRepository;

    public LogoutController(OAuthClientRepository oAuthClientRepository) {
        this.oAuthClientRepository = oAuthClientRepository;
    }

    @GetMapping("/v2/logout")
    @Operation(summary = "Logout", description = "Validates returnTo and redirects.")
    public ResponseEntity<Object> logout(
            @RequestParam(name = "client_id", required = false) String clientId,
            @RequestParam(name = "returnTo", required = false) String returnTo) {

        if (StringUtils.isBlank(returnTo)) {
            return ResponseEntity.noContent().build();
        }

        OAuthClient client = StringUtils.isNotBlank(clientId)
                ? oAuthClientRepository.findByClientId(clientId).orElse(null)
                : null;
        boolean allowed = client != null
                && client.getPostLogoutRedirectUris() != null
                && client.getPostLogoutRedirectUris().contains(returnTo);
        if (!allowed) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("error", OAuthErrors.INVALID_REQUEST);
            body.put("error_description", "returnTo is not an allowed post-logout redirect URI for this client");
            return ResponseEntity.badRequest().body(body);
        }

        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(returnTo)).build();
    }
}
