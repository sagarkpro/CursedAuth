package com.cursed.auth.config;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;

import com.cursed.auth.entities.User;
import com.cursed.auth.repository.UserRepository;

@Configuration
public class CoffeeBeans {
    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;

    public CoffeeBeans(@Value("${jwt-config.JWT_PRIVATE_KEY}") String privateKeyPem,
            @Value("${jwt-config.JWT_PUBLIC_KEY}") String publicKeyPem) throws Exception {
        privateKey = loadPrivateKey(privateKeyPem);
        publicKey = loadPublicKey(publicKeyPem);
    }

    @Bean
    public BCryptPasswordEncoder getPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JWKSource<com.nimbusds.jose.proc.SecurityContext> jwkSource() {
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();

        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                // Set this to your backend API URL!
                // For dev, it's http://localhost:7772, for prod it's
                // https://api.auth.cursedshrine.co.in
                .issuer("http://localhost:7772")
                .build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        RegisteredClient cursedFrontendClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("my-cursed-client")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)

                // 1. Where should the Auth Server send the code back to? YOUR Next.js apps!
                // Change these paths to whatever callback route your frontend is actually
                // listening on.
                .redirectUri("http://localhost:7704/api/auth/callback/cursed")
                .redirectUri("https://sso.cursedshrine.co.in/api/auth/callback/cursed")
                .redirectUri("https://oauth.pstmn.io/v1/callback")

                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("api.read")

                // 2. THIS KILLS THE CONSENT SCREEN UI
                .clientSettings(
                        ClientSettings.builder().requireProofKey(true).requireAuthorizationConsent(false).build())
                .build();

        return new InMemoryRegisteredClientRepository(cursedFrontendClient);
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<com.nimbusds.jose.proc.SecurityContext> jwkSource) {
        // This helper method takes your existing Nimbus keys and automatically
        // builds the exact decoder Spring Security is crying for.
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository repo) {
        return username -> {
            User dbUser = repo.findByEmail(username);

            if (dbUser == null) {
                throw new UsernameNotFoundException("Nice try, ghost.");
            }

            return org.springframework.security.core.userdetails.User
                    .withUsername(dbUser.getUsername())
                    .password(dbUser.getPassword())
                    .authorities(dbUser.getAuthorities())
                    .disabled(!dbUser.isVerified())
                    .accountLocked(!dbUser.isActive())
                    .build();
        };
    }

    // 3. Update the return type and cast the result
    private RSAPublicKey loadPublicKey(String pem) throws Exception {
        String publicKeyContent = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = java.util.Base64.getDecoder().decode(publicKeyContent);

        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);

        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    // 2. Update the return type and cast the result
    private RSAPrivateKey loadPrivateKey(String pem) throws Exception {
        String privateKeyContent = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", ""); // Good call wiping the newlines

        byte[] keyBytes = java.util.Base64.getDecoder().decode(privateKeyContent);

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);

        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
    }
}
