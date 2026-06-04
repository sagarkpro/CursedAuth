package com.cursed.auth.repository;

import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.stereotype.Component;

import com.cursed.auth.entities.OAuthClient;

@Component
public class MongoRegisteredClientRepository implements RegisteredClientRepository {

    private final OAuthClientRepository oAuthClientRepository;

    public MongoRegisteredClientRepository(OAuthClientRepository oAuthClientRepository) {
        this.oAuthClientRepository = oAuthClientRepository;
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        throw new UnsupportedOperationException("Unimplemented method 'save'");
    }

    @Override
    @Nullable
    public RegisteredClient findById(String id) {
        var client = oAuthClientRepository.findById(id);
        if (client.isPresent()) {
            return toRegisteredClient(client.get());
        }
        return null;
    }

    @Override
    @Nullable
    public RegisteredClient findByClientId(String clientId) {
        var client = oAuthClientRepository.findByClientId(clientId);
        if (client.isPresent()) {
            return toRegisteredClient(client.get());
        }
        return null;
    }

    private static RegisteredClient toRegisteredClient(OAuthClient client) {
        return RegisteredClient
                .withId(client.getId())
                .clientId(client.getClientId())
                .clientSecret(client.getClientSecret())
                .redirectUris(uris -> client.getRedirectUris())
                .clientSettings(
                        ClientSettings.builder()
                                .requireAuthorizationConsent(client.getRequireConsent())
                                .requireProofKey(client.getRequirePkce())
                                .build())
                .clientAuthenticationMethods(methods -> client.getAuthenticationMethods()
                        .forEach(m -> methods.add(new ClientAuthenticationMethod(m))))
                .authorizationGrantTypes(
                        grants -> client.getGrantTypes().forEach(gt -> grants.add(new AuthorizationGrantType(gt))))
                .build();
    }
}
