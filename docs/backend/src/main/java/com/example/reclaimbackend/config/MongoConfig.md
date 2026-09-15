```java
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
```

What that function does: I build the Mongo client from the resolved URI and short timeouts.
What the function is responsible for: I am the mongoClient bean the rest of Spring Data Mongo uses.
Where it works: I am created by Spring at startup. I am unused except as a bean.
Why I chose to write the code of the function the way its written: I set 10-second timeouts so a bad host fails sooner instead of hanging.
More: I use MongoClientSettings, ConnectionString, and MongoClients from the Mongo driver. TimeUnit.SECONDS sets the timeouts.

```java
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
```

What that function does: I pick a connection string from user/password/host, or fall back to MONGODB_URI, or stop if neither is set.
What the function is responsible for: I decide which URI mongoClient should use.
Where it works: I am called by mongoClient. I am unused except internally.
Why I chose to write the code of the function the way its written: I prefer separate user, password, and host so a special character in the password can be encoded. The full URI is a fallback.
More: I use Spring StringUtils.hasText so blank env values are treated as missing. @Value fields on the class feed me.

```java
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
```

What that function does: I build an Atlas mongodb+srv URI with encoded user and password.
What the function is responsible for: I format the connection string for Atlas.
Where it works: I am called by resolveConnectionString. I am unused except internally.
Why I chose to write the code of the function the way its written: I encode credentials first so @ and other characters in a password do not break the URI.
More: I use String.format and retryWrites=true&w=majority, which Atlas expects.

```java
private static String encodeMongoCredential(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
}
```

What that function does: I URL-encode a user or password and turn + into %20.
What the function is responsible for: I make credentials safe to put in a Mongo URI.
Where it works: I am called by buildAtlasUri. I am unused except internally.
Why I chose to write the code of the function the way its written: I wrote it this way because uRLEncoder turns spaces into +, which Mongo URIs do not want, so I replace that with %20.
More: I use java.net.URLEncoder and StandardCharsets.UTF_8.
