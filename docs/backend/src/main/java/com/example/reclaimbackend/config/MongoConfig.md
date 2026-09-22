# MongoConfig.java

## Overview
This is a **Spring configuration** class that builds the **MongoDB client** connection for the backend (see `docs/GLOSSARY.md`). It prefers separate env vars for user, password, and host so special characters in the password are URL-encoded safely; if those are missing, it falls back to a full `MONGODB_URI`. Timeouts are set so a dead database fails in seconds instead of hanging forever.

## Concepts used here
- **Bean** — `mongoClient()` is registered as a Spring bean; see `docs/GLOSSARY.md`.
- **MongoDB** — the database this client connects to; see `docs/GLOSSARY.md`.
- **Dependency injection / `@Value`** — Spring fills fields from environment / properties; see `docs/GLOSSARY.md`.
- **Connection string** — the URI that tells the driver where MongoDB lives and how to authenticate.
- **URL encoding** — turns special password characters into safe `%XX` form for the URI.

## Libraries and imports
- **MongoDB Java driver** (`ConnectionString`, `MongoClientSettings`, `MongoClient`, `MongoClients`): builds and creates the client with timeouts.
- **Spring** (`@Configuration`, `@Bean`, `@Value`, `StringUtils`): config class, bean method, property injection, and blank-checks.
- **Java standard** (`URLEncoder`, `StandardCharsets`, `TimeUnit`): encode credentials and express timeouts.

## Fields
- `mongoUri` (`String`) — optional full URI from `MONGODB_URI` (fallback).
- `mongoUser` (`String`) — username from `MONGODB_USER`.
- `mongoPassword` (`String`) — password from `MONGODB_PASSWORD`.
- `mongoHost` (`String`) — Atlas/host from `MONGODB_HOST`.
- `mongoDatabase` (`String`) — database name from `MONGODB_DATABASE` (default `reclaim`).

## Functions

### `private static String encodeMongoCredential(String value)`
```java
private static String encodeMongoCredential(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
}
```
- **What it does:** URL-encodes a username or password for use inside a MongoDB URI, and turns `+` into `%20` so spaces stay correct.
- **Where it's called:** only by `buildAtlasUri` in this class.
- **Why it's written this way:** passwords often contain `@`, `#`, or `/`, which would break a raw URI; encoding keeps the connection string valid.
- **Step by step:**
  1. Encode the string as UTF-8 with `URLEncoder`.
  2. Replace `+` with `%20`.
  3. Return the safe string.

### `private static String buildAtlasUri(String user, String password, String host, String database)`
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
- **What it does:** builds a standard Atlas-style `mongodb+srv://` URI from separate pieces.
- **Where it's called:** only by `resolveConnectionString` when user, password, and host are all set.
- **Why it's written this way:** splitting credentials into env vars avoids putting a fragile pre-built URI in config, and encoding happens in one place.
- **Step by step:**
  1. Encode user and password.
  2. Format them with host and database into an `mongodb+srv` URI with `retryWrites=true&w=majority`.
  3. Return that string.

### `private String resolveConnectionString()`
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
- **What it does:** picks the connection string: prefer built Atlas URI from parts, else use `MONGODB_URI`, else fail with a clear error.
- **Where it's called:** only by `mongoClient` in this class.
- **Why it's written this way:** the preferred path avoids broken URIs from special characters; the fallback supports a single full URI; failing fast is better than starting with no database.
- **Step by step:**
  1. If user, password, and host are all non-blank, build the Atlas URI.
  2. Else if `mongoUri` is set, return it.
  3. Else throw `IllegalStateException` explaining which env vars to set.

### `public MongoClient mongoClient()`
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
- **What it does:** creates the shared `MongoClient` bean Spring Data uses for all MongoDB access, with 10-second selection/connect/read timeouts.
- **Where it's called:** by Spring at startup when it needs a `MongoClient` bean. Repositories and `MongoTemplate` use that client indirectly.
- **Why it's written this way:** centralizing connection setup (URI resolution + timeouts) keeps the rest of the app from caring how credentials are provided.
- **Step by step:**
  1. Resolve the connection string.
  2. Build `MongoClientSettings` from that string plus timeouts.
  3. Create and return a `MongoClient` from those settings.
