package com.example.reclaimbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Turns on MongoDB transactions for work that must save together.
 */
@Configuration
@EnableTransactionManagement
public class MongoTransactionConfig {

    @Bean
    public MongoTransactionManager transactionManager(MongoTemplate mongoTemplate) {
        return new MongoTransactionManager(mongoTemplate.getMongoDatabaseFactory());
    }
}
