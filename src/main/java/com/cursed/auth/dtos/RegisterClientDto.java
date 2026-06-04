package com.cursed.auth.dtos;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterClientDto {
    String clientId;
    String clientSecret;

    Set<String> redirectUris;
    Set<String> scopes;
    Set<String> grantTypes;
    Set<String> authenticationMethods;

    Boolean requireConsent;
    Boolean requirePkce;
}
