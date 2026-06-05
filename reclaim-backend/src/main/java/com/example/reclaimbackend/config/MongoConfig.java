package com.example.reclaimbackend.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Builds the MongoDB Atlas connection string from separate credentials so
 * passwords with special characters (@, !, :, etc.) do not break parsing.
 */
@Configuration
public class MongoConfig {

    @Value("${MONGODB_URI:}")
    private String mongoUri;

    @Value("${MONGODB_USER:}")
    private String mongoUser;

    @Value("${MONGODB_PASSWORD:}")
    private String mongoPassword;

    @Value("${MONGODB_HOST:}")
    private String mongoHost;

    @Value("${MONGODB_DATABASE:reclaim}")
    private String mongoDatabase;

    @Bean
    public MongoClient mongoClient() {
        String connectionString = resolveConnectionString();

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .applyToClusterSettings(builder -> builder
                        .serverSelectionTimeout(10, TimeUnit.SECONDS))
                .applyToSocketSettings(builder -> builder
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(10, TimeUnit.SECONDS))
                .build();

        return MongoClients.create(settings);
    }

    private String resolveConnectionString() {
        if (StringUtils.hasText(mongoUser)
                && StringUtils.hasText(mongoPassword)
                && StringUtils.hasText(mongoHost)) {
            return buildAtlasUri(mongoUser, mongoPassword, mongoHost, mongoDatabase);
        }

        if (StringUtils.hasText(mongoUri)) {
            return mongoUri;
        }

        throw new IllegalStateException(
                "MongoDB is not configured. Set MONGODB_USER, MONGODB_PASSWORD, "
                        + "and MONGODB_HOST in the project-root .env file.");
    }

    private static String buildAtlasUri(String user, String password, String host, String database) {
        String encodedUser = encodeMongoCredential(user);
        String encodedPassword = encodeMongoCredential(password);

        return String.format(
                "mongodb+srv://%s:%s@%s/%s?retryWrites=true&w=majority",
                encodedUser,
                encodedPassword,
                host,
                database);
    }

    private static String encodeMongoCredential(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
