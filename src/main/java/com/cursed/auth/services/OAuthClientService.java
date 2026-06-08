package com.cursed.auth.services;

import com.cursed.auth.dtos.RegisterClientDto;
import com.cursed.auth.dtos.response.BaseResponseDTO;
import com.cursed.auth.entities.OAuthClient;

public interface OAuthClientService {
    BaseResponseDTO<OAuthClient> getClientByClientId(String clientId);

    BaseResponseDTO<OAuthClient> createClient(RegisterClientDto client);

    BaseResponseDTO<OAuthClient> updateClient(String clientId, RegisterClientDto client);

    BaseResponseDTO<OAuthClient> deleteClient(String clientId);
}
