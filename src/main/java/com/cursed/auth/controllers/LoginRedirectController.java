package com.cursed.auth.controllers;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cursed.auth.utils.RedirectUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Hosted-login entrypoint. /authorize redirects here; this forwards to the external React
 * login app (configured via sso.login.frontend-url), carrying the login_id. Keeping this
 * indirection means /authorize never hardcodes where the UI lives.
 */
@RestController
@Tag(name = "Hosted Login", description = "Redirects to the hosted login UI.")
public class LoginRedirectController {

    private final String frontendUrl;

    public LoginRedirectController(@Value("${sso.login.frontend-url}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    @GetMapping("/login")
    @Operation(summary = "Redirect to hosted login UI")
    public ResponseEntity<Void> login(@RequestParam(name = "login_id", required = false) String loginId) {
        Map<String, String> params = new LinkedHashMap<>();
        if (StringUtils.isNotBlank(loginId)) {
            params.put("login_id", loginId);
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(RedirectUtils.withParams(frontendUrl, params)).build();
    }
}
