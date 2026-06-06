package com.cursed.auth.repository;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.cursed.auth.entities.AuthorizationCode;

public class AuthorizationCodeRepositoryImpl implements AuthorizationCodeRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public AuthorizationCodeRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public AuthorizationCode consumeOnce(String code) {
        Query query = Query.query(Criteria.where("_id").is(code).and("consumed").is(false));
        Update update = new Update().set("consumed", true);
        // returnNew(false) -> return the document as it was BEFORE the update (the usable pre-image)
        return mongoTemplate.findAndModify(query, update,
                FindAndModifyOptions.options().returnNew(false), AuthorizationCode.class);
    }
}
