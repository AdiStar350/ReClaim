package com.example.reclaimbackend.config;

import com.example.reclaimbackend.model.Item;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

/**
 * Ensures compound indexes exist on the items collection for efficient
 * multi-parameter search and filtering.
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class MongoIndexConfig {

    private final MongoTemplate mongoTemplate;

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
            log.warn("Could not create MongoDB indexes on startup: {}", ex.getMessage());
        }
    }

}
