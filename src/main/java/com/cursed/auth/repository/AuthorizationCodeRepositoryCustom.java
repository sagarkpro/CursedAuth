package com.cursed.auth.repository;

import com.cursed.auth.entities.AuthorizationCode;

public interface AuthorizationCodeRepositoryCustom {

    /**
     * Atomically marks the code as consumed and returns its pre-image. Returns
     * {@code null} if the code does not exist or was already consumed (TTL-expired
     * codes are simply absent). This is the single-use enforcement point.
     */
    AuthorizationCode consumeOnce(String code);
}
