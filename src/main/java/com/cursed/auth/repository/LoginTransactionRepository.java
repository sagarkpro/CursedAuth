package com.cursed.auth.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.cursed.auth.entities.LoginTransaction;

@Repository
public interface LoginTransactionRepository extends MongoRepository<LoginTransaction, String> {
}
