package com.cursed.auth.services;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.cursed.auth.constants.OAuthErrors;
import com.cursed.auth.dtos.TokenRequest;
import com.cursed.auth.entities.AuthorizationCode;
import com.cursed.auth.entities.LoginTransaction;
import com.cursed.auth.entities.OAuthClient;
import com.cursed.auth.entities.RefreshToken;
import com.cursed.auth.entities.User;
import com.cursed.auth.exceptions.OAuthException;
import com.cursed.auth.repository.AuthorizationCodeRepository;
import com.cursed.auth.repository.OAuthClientRepository;
import com.cursed.auth.repository.RefreshTokenRepository;
import com.cursed.auth.repository.UserRepository;
import com.cursed.auth.utils.PkceUtils;
import com.cursed.auth.utils.TokenHashUtils;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

/**
 * Mints RS256 access/id tokens, issues authorization codes, and issues/rotates refresh
 * tokens. This is the heart of the Auth0-style flow; it deliberately produces Auth0-shaped
 * token responses (snake_case keys, "Bearer", expires_in in seconds) as a Map so the wire
 * format is exact and optional fields are simply omitted.
 */
@Service
public class TokenService {

    private static final long DEFAULT_ACCESS_TTL = 86400;        // 1 day
    private static final long DEFAULT_REFRESH_TTL = 1209600;     // 14 days
    private static final String ROLES_CLAIM = "https://cursedshrine.co.in/roles";

    private final JwtEncoder jwtEncoder;
    private final AuthorizationCodeRepository authorizationCodeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OAuthClientRepository oAuthClientRepository;
    private final UserRepository userRepository;
    private final String issuer;
    private final String signingKid;

    public TokenService(JwtEncoder jwtEncoder,
            AuthorizationCodeRepository authorizationCodeRepository,
            RefreshTokenRepository refreshTokenRepository,
            OAuthClientRepository oAuthClientRepository,
            UserRepository userRepository,
            JWKSource<SecurityContext> jwkSource,
            @Value("${sso.issuer}") String issuer) {
        this.jwtEncoder = jwtEncoder;
        this.authorizationCodeRepository = authorizationCodeRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.oAuthClientRepository = oAuthClientRepository;
        this.userRepository = userRepository;
        this.issuer = issuer;
        this.signingKid = resolveKid(jwkSource);
    }

    // ---- authorization code issuance (called from the login flow) ----

    public String issueAuthorizationCode(LoginTransaction tx, String userId, Instant authTime) {
        String code = TokenHashUtils.randomUrlSafeToken(32);
        AuthorizationCode authCode = AuthorizationCode.builder()
                .code(code)
                .clientId(tx.getClientId())
                .userId(userId)
                .redirectUri(tx.getRedirectUri())
                .scopes(tx.getScopes())
                .nonce(tx.getNonce())
                .codeChallenge(tx.getCodeChallenge())
                .codeChallengeMethod(tx.getCodeChallengeMethod())
                .audience(tx.getAudience())
                .authTime(authTime)
                .consumed(false)
                .createdAt(Instant.now())
                .build();
        authorizationCodeRepository.save(authCode);
        return code;
    }

    // ---- /oauth/token entrypoint ----

    public Map<String, Object> token(TokenRequest req) {
        if (StringUtils.isBlank(req.grantType())) {
            throw new OAuthException(OAuthErrors.INVALID_REQUEST, "Missing 'grant_type'", HttpStatus.BAD_REQUEST);
        }
        return switch (req.grantType()) {
            case "authorization_code" -> exchangeAuthorizationCode(req);
            case "refresh_token" -> refresh(req);
            default -> throw new OAuthException(OAuthErrors.UNSUPPORTED_GRANT_TYPE,
                    "Unsupported grant_type: " + req.grantType(), HttpStatus.BAD_REQUEST);
        };
    }

    private Map<String, Object> exchangeAuthorizationCode(TokenRequest req) {
        if (StringUtils.isBlank(req.code())) {
            throw new OAuthException(OAuthErrors.INVALID_REQUEST, "Missing 'code'", HttpStatus.BAD_REQUEST);
        }
        if (StringUtils.isBlank(req.clientId())) {
            throw new OAuthException(OAuthErrors.INVALID_REQUEST, "Missing 'client_id'", HttpStatus.BAD_REQUEST);
        }
        OAuthClient client = loadActiveClient(req.clientId());
        authenticateClientIfConfidential(client, req);

        // single-use: atomically consume; null => unknown/already-used/expired
        AuthorizationCode code = authorizationCodeRepository.consumeOnce(req.code());
        if (code == null) {
            throw new OAuthException(OAuthErrors.INVALID_GRANT,
                    "Invalid or expired authorization code", HttpStatus.BAD_REQUEST);
        }
        if (!client.getClientId().equals(code.getClientId())) {
            throw new OAuthException(OAuthErrors.INVALID_GRANT, "client_id mismatch", HttpStatus.BAD_REQUEST);
        }
        if (StringUtils.isBlank(req.redirectUri()) || !req.redirectUri().equals(code.getRedirectUri())) {
            throw new OAuthException(OAuthErrors.INVALID_GRANT, "redirect_uri mismatch", HttpStatus.BAD_REQUEST);
        }
        if (!PkceUtils.verifyS256(req.codeVerifier(), code.getCodeChallenge())) {
            throw new OAuthException(OAuthErrors.INVALID_GRANT, "PKCE verification failed", HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findById(code.getUserId())
                .orElseThrow(() -> new OAuthException(OAuthErrors.INVALID_GRANT, "User not found",
                        HttpStatus.BAD_REQUEST));

        Map<String, Object> response = buildTokenResponse(client, user, code.getScopes(),
                code.getAudience(), code.getNonce(), code.getAuthTime());

        if (code.getScopes() != null && code.getScopes().contains("offline_access")) {
            response.put("refresh_token", issueNewRefreshToken(client, user, code.getScopes(), code.getAudience()));
        }
        return response;
    }

    private Map<String, Object> refresh(TokenRequest req) {
        if (StringUtils.isBlank(req.refreshToken())) {
            throw new OAuthException(OAuthErrors.INVALID_REQUEST, "Missing 'refresh_token'", HttpStatus.BAD_REQUEST);
        }
        if (StringUtils.isBlank(req.clientId())) {
            throw new OAuthException(OAuthErrors.INVALID_REQUEST, "Missing 'client_id'", HttpStatus.BAD_REQUEST);
        }
        OAuthClient client = loadActiveClient(req.clientId());
        authenticateClientIfConfidential(client, req);

        String presentedHash = TokenHashUtils.sha256Hex(req.refreshToken());
        RefreshToken existing = refreshTokenRepository.findByTokenHash(presentedHash)
                .orElseThrow(() -> new OAuthException(OAuthErrors.INVALID_GRANT, "Invalid refresh token",
                        HttpStatus.BAD_REQUEST));

        if (!client.getClientId().equals(existing.getClientId())) {
            throw new OAuthException(OAuthErrors.INVALID_GRANT, "client_id mismatch", HttpStatus.BAD_REQUEST);
        }
        // reuse detection: a revoked or already-rotated token means theft -> nuke the family
        if (existing.isRevoked() || existing.getReplacedByHash() != null) {
            refreshTokenRepository.revokeFamily(existing.getFamilyId());
            throw new OAuthException(OAuthErrors.INVALID_GRANT, "Refresh token reuse detected",
                    HttpStatus.BAD_REQUEST);
        }
        if (existing.getExpiresAt() != null && existing.getExpiresAt().isBefore(Instant.now())) {
            throw new OAuthException(OAuthErrors.INVALID_GRANT, "Refresh token expired", HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findById(existing.getUserId())
                .orElseThrow(() -> new OAuthException(OAuthErrors.INVALID_GRANT, "User not found",
                        HttpStatus.BAD_REQUEST));

        Set<String> scopes = existing.getScopes();
        String audience = existing.getAudience();

        String refreshTokenValue;
        if (Boolean.TRUE.equals(client.getReuseRefreshTokens())) {
            refreshTokenValue = req.refreshToken(); // non-rotating: hand back the same token
        } else {
            String newValue = TokenHashUtils.randomUrlSafeToken(32);
            String newHash = TokenHashUtils.sha256Hex(newValue);
            RefreshToken claimed = refreshTokenRepository.claimForRotation(presentedHash, newHash);
            if (claimed == null) {
                // lost the race -> concurrent rotation == reuse
                refreshTokenRepository.revokeFamily(existing.getFamilyId());
                throw new OAuthException(OAuthErrors.INVALID_GRANT, "Refresh token reuse detected",
                        HttpStatus.BAD_REQUEST);
            }
            refreshTokenRepository.save(RefreshToken.builder()
                    .id(UUID.randomUUID().toString())
                    .tokenHash(newHash)
                    .clientId(client.getClientId())
                    .userId(user.getId())
                    .audience(audience)
                    .scopes(scopes)
                    .familyId(existing.getFamilyId())
                    .revoked(false)
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(refreshTtl(client)))
                    .build());
            refreshTokenValue = newValue;
        }

        Map<String, Object> response = buildTokenResponse(client, user, scopes, audience, null, Instant.now());
        response.put("refresh_token", refreshTokenValue);
        return response;
    }

    // ---- token building ----

    private Map<String, Object> buildTokenResponse(OAuthClient client, User user, Set<String> scopes,
            String audience, String nonce, Instant authTime) {
        Instant now = Instant.now();
        long accessTtl = accessTtl(client);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("access_token", mintAccessToken(client, user, scopes, audience, now, accessTtl));
        response.put("token_type", "Bearer");
        response.put("expires_in", accessTtl);
        response.put("scope", scopes == null ? "" : String.join(" ", scopes));

        if (scopes != null && scopes.contains("openid")) {
            response.put("id_token", mintIdToken(client, user, scopes, nonce, now, authTime));
        }
        return response;
    }

    private String mintAccessToken(OAuthClient client, User user, Set<String> scopes,
            String audience, Instant now, long ttl) {
        String aud = StringUtils.isNotBlank(audience) ? audience : client.getClientId();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getId())
                .audience(List.of(aud))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(ttl))
                .id(UUID.randomUUID().toString())
                .claim("scope", scopes == null ? "" : String.join(" ", scopes))
                .claim("azp", client.getClientId());
        if (user.getRole() != null) {
            claims.claim(ROLES_CLAIM, List.of(user.getRole().toString()));
        }
        return encode(claims.build());
    }

    private String mintIdToken(OAuthClient client, User user, Set<String> scopes,
            String nonce, Instant now, Instant authTime) {
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getId())
                .audience(List.of(client.getClientId()))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(accessTtl(client)))
                .claim("azp", client.getClientId());
        if (authTime != null) {
            claims.claim("auth_time", authTime.getEpochSecond());
        }
        if (StringUtils.isNotBlank(nonce)) {
            claims.claim("nonce", nonce);
        }
        if (scopes != null && scopes.contains("profile")) {
            claims.claim("name", user.getDisplayName());
            if (StringUtils.isNotBlank(user.getFirstName())) {
                claims.claim("given_name", user.getFirstName());
            }
            if (StringUtils.isNotBlank(user.getLastName())) {
                claims.claim("family_name", user.getLastName());
            }
            if (StringUtils.isNotBlank(user.getMiddleName())) {
                claims.claim("middle_name", user.getMiddleName());
            }
            if (StringUtils.isNotBlank(user.getProfileImage())) {
                claims.claim("picture", user.getProfileImage());
            }
            if (user.getUpdatedAt() != null) {
                claims.claim("updated_at", user.getUpdatedAt().getEpochSecond());
            }
        }
        if (scopes != null && scopes.contains("email")) {
            claims.claim("email", user.getEmail());
            claims.claim("email_verified", user.isVerified());
        }
        if (user.getRole() != null) {
            claims.claim(ROLES_CLAIM, List.of(user.getRole().toString()));
        }
        return encode(claims.build());
    }

    private String encode(JwtClaimsSet claims) {
        JwsHeader.Builder header = JwsHeader.with(SignatureAlgorithm.RS256);
        if (signingKid != null) {
            header.keyId(signingKid);
        }
        return jwtEncoder.encode(JwtEncoderParameters.from(header.build(), claims)).getTokenValue();
    }

    // ---- helpers ----

    private String issueNewRefreshToken(OAuthClient client, User user, Set<String> scopes, String audience) {
        String value = TokenHashUtils.randomUrlSafeToken(32);
        refreshTokenRepository.save(RefreshToken.builder()
                .id(UUID.randomUUID().toString())
                .tokenHash(TokenHashUtils.sha256Hex(value))
                .clientId(client.getClientId())
                .userId(user.getId())
                .audience(audience)
                .scopes(scopes)
                .familyId(UUID.randomUUID().toString())
                .revoked(false)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(refreshTtl(client)))
                .build());
        return value;
    }

    private OAuthClient loadActiveClient(String clientId) {
        OAuthClient client = oAuthClientRepository.findByClientId(clientId)
                .orElseThrow(() -> new OAuthException(OAuthErrors.INVALID_CLIENT, "Unknown client",
                        HttpStatus.UNAUTHORIZED));
        if (StringUtils.isNotBlank(client.getStatus()) && !"ACTIVE".equalsIgnoreCase(client.getStatus())) {
            throw new OAuthException(OAuthErrors.INVALID_CLIENT, "Client is not active", HttpStatus.UNAUTHORIZED);
        }
        return client;
    }

    private void authenticateClientIfConfidential(OAuthClient client, TokenRequest req) {
        boolean isPublic = client.getAuthenticationMethods() == null
                || client.getAuthenticationMethods().isEmpty()
                || client.getAuthenticationMethods().contains("none");
        if (isPublic) {
            return; // SPA clients authenticate by PKCE only
        }
        if (StringUtils.isBlank(req.clientSecret()) || !req.clientSecret().equals(client.getClientSecret())) {
            throw new OAuthException(OAuthErrors.INVALID_CLIENT, "Invalid client credentials",
                    HttpStatus.UNAUTHORIZED);
        }
    }

    private long accessTtl(OAuthClient client) {
        return client.getAccessTokenTtlSeconds() > 0 ? client.getAccessTokenTtlSeconds() : DEFAULT_ACCESS_TTL;
    }

    private long refreshTtl(OAuthClient client) {
        return client.getRefreshTokenTtlSeconds() > 0 ? client.getRefreshTokenTtlSeconds() : DEFAULT_REFRESH_TTL;
    }

    private static String resolveKid(JWKSource<SecurityContext> jwkSource) {
        try {
            List<JWK> keys = jwkSource.get(
                    new JWKSelector(new JWKMatcher.Builder().keyUse(KeyUse.SIGNATURE).build()), null);
            return keys.isEmpty() ? null : keys.get(0).getKeyID();
        } catch (KeySourceException e) {
            throw new IllegalStateException("Unable to resolve signing key id", e);
        }
    }
}
