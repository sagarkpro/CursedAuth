package com.cursed.auth.utils;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.cursed.auth.entities.User;
import com.cursed.auth.repository.UserRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

@Component
public class JwtUtils {
    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final long expirationMillis;
    private final UserRepository userRepository;

    public JwtUtils(@Value("${jwt-config.JWT_PRIVATE_KEY}") String privateKeyPem,
            @Value("${jwt-config.JWT_PUBLIC_KEY}") String publicKeyPem,
            @Value("${jwt-config.JWT_EXPIRY}") long expirationMillis, UserRepository userRepository) throws Exception {
        this.expirationMillis = expirationMillis;
        this.userRepository = userRepository;
        this.privateKey = loadPrivateKey(privateKeyPem);
        this.publicKey = loadPublicKey(publicKeyPem);
    }

    public String generateToken(User user) {
        return generateTimedToken(user, expirationMillis);
    }

    public String generateShortLivedToken(User user) {
        long tenMins = 10L * 60 * 1000;
        return generateTimedToken(user, tenMins);
    }

    public boolean verifySignature(String token) {
        if (StringUtils.isBlank(token)) {
            return false;
        }

        try {
            Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token);

            return true;
        } catch (JwtException | IllegalArgumentException _) {
            return false;
        }
    }

    public User getUserFromToken(String token) {
        if (StringUtils.isBlank(token)) {
            throw new IllegalArgumentException("Missing JWT token");
        }

        Claims claims = Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        String userEmail = claims.getSubject();
        var user = userRepository.findByEmail(userEmail);
        if (user == null) {
            throw new AuthenticationCredentialsNotFoundException("Invalid email or password");
        }
        return user;
    }

    public static Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        var user = auth != null ? (User) auth.getPrincipal() : null;
        return Optional.of(user);
    }

    private String generateTimedToken(User user, long life) {
        Instant now = Instant.now();
        var claims = Map.of(
                "role", user.getRole().name(),
                "userId", user.getId(),
                "userTag", user.getUsername(),
                "displayName", user.getDisplayName());

        if (StringUtils.isNotEmpty(user.getProfileImage())) {
            claims.put("profileImage", user.getProfileImage());
        }

        return Jwts.builder()
                .subject(user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(life)))
                .claims(claims)
                .signWith(privateKey, Jwts.SIG.RS512)
                .compact();
    }

    private PrivateKey loadPrivateKey(String pem) throws Exception {
        String privateKeyContent = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = java.util.Base64.getDecoder().decode(privateKeyContent);

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);

        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private PublicKey loadPublicKey(String pem) throws Exception {
        String publicKeyContent = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = java.util.Base64.getDecoder().decode(publicKeyContent);

        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);

        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }
}
