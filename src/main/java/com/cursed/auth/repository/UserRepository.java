package com.cursed.auth.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.cursed.auth.entities.User;

public interface UserRepository extends MongoRepository<User, String> {

    // custom query methods (optional)
    User findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
