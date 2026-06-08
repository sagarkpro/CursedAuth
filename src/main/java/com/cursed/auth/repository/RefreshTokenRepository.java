package com.cursed.auth.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.cursed.auth.entities.RefreshToken;

@Repository
public interface RefreshTokenRepository
        extends MongoRepository<RefreshToken, String>, RefreshTokenRepositoryCustom {

    Optional<RefreshToken> findByTokenHash(String tokenHash);
}
