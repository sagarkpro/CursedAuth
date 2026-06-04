package com.cursed.auth.config;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import com.cursed.auth.repository.MongoRegisteredClientRepository;

@Configuration
public class CoffeeBeans {
    @Bean
    public BCryptPasswordEncoder getPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(MongoRegisteredClientRepository repository) {
        return repository;
    }
}
