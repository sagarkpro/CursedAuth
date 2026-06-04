package com.cursed.auth.entities;

import java.util.Set;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "oauth_clients")
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class OAuthClient extends BaseEntity {
    String clientId;
    String clientSecret;

    Set<String> redirectUris;
    Set<String> scopes;
    Set<String> grantTypes;
    Set<String> authenticationMethods;

    Boolean requireConsent;
    Boolean requirePkce;
}