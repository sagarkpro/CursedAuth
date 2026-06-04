package com.cursed.auth.services;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cursed.auth.dtos.RegisterClientDto;
import com.cursed.auth.dtos.response.BaseResponseDTO;
import com.cursed.auth.dtos.response.ErrorDTO;
import com.cursed.auth.entities.OAuthClient;
import com.cursed.auth.repository.OAuthClientRepository;

@Service
public class OAuthClientServiceImpl implements OAuthClientService {
    private final OAuthClientRepository oAuthClientRepository;

    public OAuthClientServiceImpl(OAuthClientRepository oAuthClientRepository) {
        this.oAuthClientRepository = oAuthClientRepository;
    }

    @Override
    public BaseResponseDTO<OAuthClient> getClientByClientId(String clientId) {
        return oAuthClientRepository.findByClientId(clientId)
                .map(client -> BaseResponseDTO.<OAuthClient>builder()
                        .success(true)
                        .data(client)
                        .build())
                .orElseGet(() -> notFoundResponse(clientId));
    }

    @Override
    public BaseResponseDTO<OAuthClient> createClient(RegisterClientDto client) {
        if (oAuthClientRepository.findByClientId(client.getClientId()).isPresent()) {
            return BaseResponseDTO.<OAuthClient>builder()
                    .success(false)
                    .error(ErrorDTO.builder()
                            .message("OAuth client already exists")
                            .status(HttpStatus.CONFLICT)
                            .build())
                    .build();
        }

        return BaseResponseDTO.<OAuthClient>builder()
                .success(true)
                .data(oAuthClientRepository.save(buildOAuthClient(client)))
                .build();
    }

    @Override
    public BaseResponseDTO<OAuthClient> updateClient(String clientId, RegisterClientDto client) {
        var existingClient = oAuthClientRepository.findByClientId(clientId);
        if (existingClient.isEmpty()) {
            return notFoundResponse(clientId);
        }

        return BaseResponseDTO.<OAuthClient>builder()
                .success(true)
                .data(oAuthClientRepository.save(buildOAuthClient(client, existingClient.get())))
                .build();
    }

    @Override
    public BaseResponseDTO<OAuthClient> deleteClient(String clientId) {
        var existingClient = oAuthClientRepository.findByClientId(clientId);
        if (existingClient.isEmpty()) {
            return notFoundResponse(clientId);
        }

        OAuthClient client = existingClient.get();
        oAuthClientRepository.delete(client);

        return BaseResponseDTO.<OAuthClient>builder()
                .success(true)
                .data(client)
                .build();
    }

    private BaseResponseDTO<OAuthClient> notFoundResponse(String clientId) {
        return BaseResponseDTO.<OAuthClient>builder()
                .success(false)
                .error(ErrorDTO.builder()
                        .message("OAuth client not found: " + clientId)
                        .status(HttpStatus.NOT_FOUND)
                        .build())
                .build();
    }

    private OAuthClient buildOAuthClient(RegisterClientDto client) {
        return OAuthClient.builder()
                .createdAt(Instant.now())
                .clientId(client.getClientId())
                .clientSecret(client.getClientSecret())
                .redirectUris(client.getRedirectUris())
                .scopes(client.getScopes())
                .grantTypes(client.getGrantTypes())
                .authenticationMethods(client.getAuthenticationMethods())
                .requireConsent(client.getRequireConsent())
                .requirePkce(client.getRequirePkce())
                .build();
    }

    private OAuthClient buildOAuthClient(RegisterClientDto client, OAuthClient existingClient) {
        return OAuthClient.builder()
                .id(existingClient.getId())
                .createdAt(existingClient.getCreatedAt())
                .updatedAt(Instant.now())
                .updatedBy(existingClient.getUpdatedBy())
                .clientId(client.getClientId())
                .clientSecret(client.getClientSecret())
                .redirectUris(client.getRedirectUris())
                .scopes(client.getScopes())
                .grantTypes(client.getGrantTypes())
                .authenticationMethods(client.getAuthenticationMethods())
                .requireConsent(client.getRequireConsent())
                .requirePkce(client.getRequirePkce())
                .build();
    }
}
