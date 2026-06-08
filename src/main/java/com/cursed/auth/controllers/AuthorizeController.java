package com.cursed.auth.controllers;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cursed.auth.constants.OAuthErrors;
import com.cursed.auth.entities.LoginTransaction;
import com.cursed.auth.entities.OAuthClient;
import com.cursed.auth.repository.LoginTransactionRepository;
import com.cursed.auth.repository.OAuthClientRepository;
import com.cursed.auth.utils.RedirectUtils;
import com.cursed.auth.utils.TokenHashUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * OAuth 2.1 Authorization Endpoint. This is the URL the Auth0 React SDK navigates the
 * browser to. It does NOT render a UI: it validates the client + redirect_uri + PKCE,
 * stores the request as a short-lived login transaction, and 302-redirects to the hosted
 * login route (/login), which in turn forwards to the React login app.
 */
@RestController
@Tag(name = "OAuth Authorize", description = "OAuth 2.1 authorization endpoint (PKCE).")
public class AuthorizeController {

    private final OAuthClientRepository oAuthClientRepository;
    private final LoginTransactionRepository loginTransactionRepository;

    public AuthorizeController(OAuthClientRepository oAuthClientRepository,
            LoginTransactionRepository loginTransactionRepository) {
        this.oAuthClientRepository = oAuthClientRepository;
        this.loginTransactionRepository = loginTransactionRepository;
    }

    @GetMapping("/authorize")
    @Operation(summary = "Authorization endpoint", description = "Validates the request and redirects to the login UI.")
    public ResponseEntity<Object> authorize(
            @RequestParam(name = "response_type", required = false) String responseType,
            @RequestParam(name = "client_id", required = false) String clientId,
            @RequestParam(name = "redirect_uri", required = false) String redirectUri,
            @RequestParam(name = "scope", required = false) String scope,
            @RequestParam(name = "state", required = false) String state,
            @RequestParam(name = "nonce", required = false) String nonce,
            @RequestParam(name = "code_challenge", required = false) String codeChallenge,
            @RequestParam(name = "code_challenge_method", required = false) String codeChallengeMethod,
            @RequestParam(name = "audience", required = false) String audience) {

        // 1. Client must exist and be active. We cannot trust redirect_uri yet, so errors
        //    here are rendered directly (never redirected).
        if (StringUtils.isBlank(clientId)) {
            return plainError(OAuthErrors.INVALID_REQUEST, "Missing client_id");
        }
        OAuthClient client = oAuthClientRepository.findByClientId(clientId).orElse(null);
        if (client == null) {
            return plainError(OAuthErrors.INVALID_REQUEST, "Unknown client_id");
        }
        if (StringUtils.isNotBlank(client.getStatus()) && !"ACTIVE".equalsIgnoreCase(client.getStatus())) {
            return plainError(OAuthErrors.INVALID_REQUEST, "Client is not active");
        }

        // 2. redirect_uri must be registered. Until validated, errors are rendered directly.
        if (StringUtils.isBlank(redirectUri)
                || client.getRedirectUris() == null
                || !client.getRedirectUris().contains(redirectUri)) {
            return plainError(OAuthErrors.INVALID_REQUEST, "Invalid redirect_uri");
        }

        // 3. From here redirect_uri is trusted -> protocol errors go back to the client.
        if (!"code".equals(responseType)) {
            return redirectError(redirectUri, state, OAuthErrors.UNSUPPORTED_RESPONSE_TYPE,
                    "Only response_type=code is supported");
        }
        // 4. PKCE is mandatory (OAuth 2.1); only S256 is accepted.
        if (StringUtils.isBlank(codeChallenge) || !"S256".equals(codeChallengeMethod)) {
            return redirectError(redirectUri, state, OAuthErrors.INVALID_REQUEST,
                    "PKCE with code_challenge_method=S256 is required");
        }
        // 5. Requested scopes must be allowed for the client (when the client declares scopes).
        Set<String> requestedScopes = parseScopes(scope);
        if (client.getScopes() != null && !client.getScopes().isEmpty()
                && !client.getScopes().containsAll(requestedScopes)) {
            return redirectError(redirectUri, state, OAuthErrors.INVALID_SCOPE,
                    "One or more requested scopes are not allowed for this client");
        }

        // 6. Persist the login transaction and hand a login_id to the login UI.
        LoginTransaction tx = LoginTransaction.builder()
                .id(TokenHashUtils.randomUrlSafeToken(24))
                .clientId(clientId)
                .redirectUri(redirectUri)
                .scopes(requestedScopes)
                .state(state)
                .nonce(nonce)
                .codeChallenge(codeChallenge)
                .codeChallengeMethod(codeChallengeMethod)
                .audience(audience)
                .responseType(responseType)
                .createdAt(Instant.now())
                .build();
        loginTransactionRepository.save(tx);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(RedirectUtils.withParams("/login", Map.of("login_id", tx.getId()))).build();
    }

    private static Set<String> parseScopes(String scope) {
        if (StringUtils.isBlank(scope)) {
            return new LinkedHashSet<>(List.of("openid"));
        }
        return new LinkedHashSet<>(Arrays.asList(scope.trim().split("\\s+")));
    }

    private ResponseEntity<Object> plainError(String error, String description) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("error_description", description);
        return ResponseEntity.badRequest().body(body);
    }

    private ResponseEntity<Object> redirectError(String redirectUri, String state, String error, String description) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("error", error);
        params.put("error_description", description);
        if (StringUtils.isNotBlank(state)) {
            params.put("state", state);
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(RedirectUtils.withParams(redirectUri, params)).build();
    }
}
