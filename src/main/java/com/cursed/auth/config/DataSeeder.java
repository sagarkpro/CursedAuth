package com.cursed.auth.config;

import java.time.Instant;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.cursed.auth.entities.OAuthClient;
import com.cursed.auth.entities.User;
import com.cursed.auth.entities.enums.Role;
import com.cursed.auth.logging.CursedLogger;
import com.cursed.auth.repository.OAuthClientRepository;
import com.cursed.auth.repository.UserRepository;

/**
 * Seeds the default OAuth client and a verified admin user on startup.
 * Idempotent:
 * creates each only if absent, so it is safe to run on every boot.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    @Value("${seed-config.client-id}")
    private String clientId;

    @Value("${seed-config.admin-email}")
    private String adminEmail;

    @Value("${seed-config.admin-username}")
    private String adminUsername;

    @Value("${seed-config.admin-password}")
    private String adminPassword;

    @Value("${seed-config.redirect-uris}")
    private String[] redirectUris;

    @Value("${seed-config.logout-uris}")
    private String[] logoutUris;

    private final OAuthClientRepository oAuthClientRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public DataSeeder(OAuthClientRepository oAuthClientRepository, UserRepository userRepository,
            BCryptPasswordEncoder passwordEncoder) {
        this.oAuthClientRepository = oAuthClientRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedClient();
        seedAdminUser();
    }

    private void seedClient() {
        if (oAuthClientRepository.findByClientId(clientId).isPresent()) {
            CursedLogger.info("Seed: OAuth client '" + clientId + "' already exists, skip");
            return;
        }

        OAuthClient client = OAuthClient.builder()
                .createdAt(Instant.now())
                .clientId(clientId)
                .redirectUris(Set.of(redirectUris))
                .scopes(Set.of("openid", "profile", "email", "offline_access"))
                .grantTypes(Set.of("authorization_code", "refresh_token"))
                .authenticationMethods(Set.of("none"))
                .postLogoutRedirectUris(Set.of(logoutUris))
                .accessTokenTtlSeconds(86400)
                .refreshTokenTtlSeconds(1209600)
                .reuseRefreshTokens(false)
                .requirePkce(true)
                .requireConsent(false)
                .status("ACTIVE")
                .build();

        oAuthClientRepository.save(client);
        CursedLogger.info("Seed: created OAuth client '" + clientId + "'");
    }

    private void seedAdminUser() {
        if (userRepository.existsByEmail(adminEmail)) {
            CursedLogger.info("Seed: admin user '" + adminEmail + "' already exists, skip");
            return;
        }

        User admin = User.builder()
                .createdAt(Instant.now())
                .email(adminEmail)
                .username(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .firstName("Cursed")
                .lastName("Admin")
                .role(Role.SUPERUSER)
                .verified(true)
                .isActive(true)
                .build();

        userRepository.save(admin);
        CursedLogger.info("Seed: created admin user '" + adminEmail + "'");
    }
}
