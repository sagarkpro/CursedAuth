package com.cursed.auth.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.cursed.auth.entities.AuthorizationCode;

@Repository
public interface AuthorizationCodeRepository
        extends MongoRepository<AuthorizationCode, String>, AuthorizationCodeRepositoryCustom {
}
