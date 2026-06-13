package com.cursed.auth.config;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactory;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

@Configuration
public class CoffeeBeans {
    RSAPrivateKey privateKey;
    RSAPublicKey publicKey;

    public CoffeeBeans(@Value("${jwt-config.JWT_PRIVATE_KEY}") String privateKeyPem,
            @Value("${jwt-config.JWT_PUBLIC_KEY}") String publicKeyPem)
            throws Exception {
        this.privateKey = loadPrivateKey(privateKeyPem);
        this.publicKey = loadPublicKey(publicKeyPem);
    }

    @Bean
    public BCryptPasswordEncoder getPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Signing key set for our RS256 access/id tokens. The kid is derived from the
     * RFC 7638 thumbprint so it is deterministic across restarts and always matches
     * what we publish at /.well-known/jwks.json.
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() throws JOSEException {
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyIDFromThumbprint()
                .build();
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    /**
     * Decoder used by the resource server to validate access tokens on our own
     * protected APIs. Verifies signature + exp/nbf only (no iss/aud), which lets
     * the
     * trailing-slash Auth0-style issuer pass through cleanly.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withPublicKey(publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
    }

    private RSAPrivateKey loadPrivateKey(String pem) throws Exception {
        String privateKeyContent = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = java.util.Base64.getDecoder().decode(privateKeyContent);

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);

        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private RSAPublicKey loadPublicKey(String pem) throws Exception {
        String publicKeyContent = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = java.util.Base64.getDecoder().decode(publicKeyContent);

        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);

        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
    }
}
