package com.example.reclaimbackend.config;

import com.example.reclaimbackend.model.Item;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

/**
 * Ensures compound indexes exist on the items collection for efficient
 * multi-parameter search and filtering.
 */
@Configuration
public class MongoIndexConfig {

    private static final Logger logger = LoggerFactory.getLogger(MongoIndexConfig.class);

    private final MongoTemplate mongoTemplate;

    public MongoIndexConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void initIndexes() {
        try {
            mongoTemplate.indexOps(Item.class)
                    .ensureIndex(new Index()
                            .on("category", Sort.Direction.ASC)
                            .on("status", Sort.Direction.ASC)
                            .on("type", Sort.Direction.ASC)
                            .on("reportedAt", Sort.Direction.DESC));

            mongoTemplate.indexOps(Item.class)
                    .ensureIndex(new Index()
                            .on("latitude", Sort.Direction.ASC)
                            .on("longitude", Sort.Direction.ASC));
        } catch (Exception ex) {
            logger.warn("Could not create MongoDB indexes on startup: {}", ex.getMessage());
        }
    }
}
