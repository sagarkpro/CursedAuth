package com.cursed.auth.repository;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.cursed.auth.entities.RefreshToken;

public class RefreshTokenRepositoryImpl implements RefreshTokenRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public RefreshTokenRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public RefreshToken claimForRotation(String tokenHash, String replacedByHash) {
        Query query = Query.query(Criteria.where("tokenHash").is(tokenHash)
                .and("revoked").is(false)
                .and("replacedByHash").is(null));
        Update update = new Update()
                .set("replacedByHash", replacedByHash)
                .set("revoked", true);
        return mongoTemplate.findAndModify(query, update,
                FindAndModifyOptions.options().returnNew(false), RefreshToken.class);
    }

    @Override
    public void revokeFamily(String familyId) {
        Query query = Query.query(Criteria.where("familyId").is(familyId));
        Update update = new Update().set("revoked", true);
        mongoTemplate.updateMulti(query, update, RefreshToken.class);
    }
}
