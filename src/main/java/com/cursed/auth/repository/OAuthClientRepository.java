package com.cursed.auth.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.cursed.auth.entities.OAuthClient;

@Repository
public interface OAuthClientRepository extends MongoRepository<OAuthClient, String> {
    Optional<OAuthClient> findByClientId(String clientId);
}
