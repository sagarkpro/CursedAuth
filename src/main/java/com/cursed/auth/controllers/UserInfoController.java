package com.cursed.auth.controllers;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cursed.auth.entities.User;
import com.cursed.auth.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * OIDC UserInfo endpoint. Requires a valid Bearer access token (resource-server
 * chain).
 * Returns claims gated by the scopes the access token was granted. The Auth0
 * React SDK
 * does not call this by default (it reads the ID token), but it is part of
 * OIDC.
 */
@RestController
@Tag(name = "OIDC UserInfo", description = "OpenID Connect UserInfo endpoint.")
public class UserInfoController {

    private final UserRepository userRepository;

    public UserInfoController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping(value = "/userinfo", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "UserInfo (Bearer access token)")
    public ResponseEntity<Map<String, Object>> userinfo(@AuthenticationPrincipal Jwt jwt) {
        User user = userRepository.findByEmail(jwt.getSubject());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String scope = jwt.getClaimAsString("scope");
        boolean hasProfile = scope != null && scope.contains("profile");
        boolean hasEmail = scope != null && scope.contains("email");

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", user.getEmail());
        if (hasProfile) {
            claims.put("name", user.getDisplayName());
            if (StringUtils.isNotBlank(user.getFirstName())) {
                claims.put("given_name", user.getFirstName());
            }
            if (StringUtils.isNotBlank(user.getLastName())) {
                claims.put("family_name", user.getLastName());
            }
            if (StringUtils.isNotBlank(user.getMiddleName())) {
                claims.put("middle_name", user.getMiddleName());
            }
            if (StringUtils.isNotBlank(user.getProfileImage())) {
                claims.put("picture", user.getProfileImage());
            }
            if (user.getUpdatedAt() != null) {
                claims.put("updated_at", user.getUpdatedAt().getEpochSecond());
            }
            if (user.getRole() != null) {
                claims.put("role", user.getRole().toString());
            }
        }
        if (hasEmail) {
            claims.put("email", user.getEmail());
            claims.put("email_verified", user.isVerified());
        }
        return ResponseEntity.ok(claims);
    }
}
