# JwtUtil.java

## Overview
This is a **helper component** (a Spring-managed `@Component` bean) that makes and checks **JWTs** — the signed login tokens the app carries to prove who it is (see `docs/GLOSSARY.md`). A "component" here just means a plain worker object Spring creates once and shares. `JwtUtil` is the single place that knows the signing key, so token creation and verification stay consistent.

## Concepts used here
- **JWT (JSON Web Token)** — a signed string carrying the user id and email; see `docs/GLOSSARY.md`.
- **Signing key / HMAC** — a secret used to sign tokens so nobody can forge one.
- **Claims** — the pieces of data inside a token (subject = user id, plus email, issue/expiry times).
- **Dependency injection with `@Value`** — Spring passes configuration values (the secret and lifetime) into the constructor; see `docs/GLOSSARY.md`.

## Libraries and imports
- **JJWT library** (`io.jsonwebtoken.Jwts`, `io.jsonwebtoken.Claims`, `io.jsonwebtoken.security.Keys`): the JWT toolkit. `Jwts.builder()` builds and signs a token; `Jwts.parser()...parseSignedClaims(...)` verifies and reads one; `Keys.hmacShaKeyFor(...)` turns the secret text into a signing key; `Claims` is the token's readable body.
- **Java crypto/standard** (`javax.crypto.SecretKey`, `java.nio.charset.StandardCharsets`, `java.util.Date`): `SecretKey` holds the signing key; `StandardCharsets.UTF_8` turns the secret string into bytes; `Date` sets issue and expiry times.
- **Spring** (`org.springframework.beans.factory.annotation.Value`, `org.springframework.stereotype.Component`): `@Component` makes this a shared bean; `@Value` injects `jwt.secret` and `jwt.expiration` from `application.properties` (which read the `JWT_SECRET` and `JWT_EXPIRATION_MS` environment values).

## Fields
- `secretKey` (`SecretKey`) — the key used to sign new tokens and verify incoming ones. Built once from the configured secret.
- `expirationMs` (`long`) — how long a token stays valid, in milliseconds.

## Functions

### `private Claims parseClaims(String token)`
```java
private Claims parseClaims(String token) {
    return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
}
```
- **What it does:** verifies a token's signature with the secret key and returns its readable body (the claims). If the signature is wrong or the token is malformed, the library throws.
- **Where it's called:** inside this class only — by `getUserIdFromToken`, `getEmailFromToken`, and `validateToken`.
- **Why it's written this way:** verification and reading always go together, so putting them in one private helper avoids repeating the parser setup and guarantees every read first checks the signature.
- **Step by step:**
  1. Create a parser configured to verify with `secretKey`.
  2. Parse the signed token; this throws if the signature or format is bad.
  3. Return the payload (the `Claims`).

### `public JwtUtil(@Value("${jwt.secret}") String secret, @Value("${jwt.expiration}") long expiration)`
```java
public JwtUtil(@Value("${jwt.secret}") String secret,
               @Value("${jwt.expiration}") long expiration) {
    this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationMs = expiration;
}
```
- **What it does:** builds the signing key from the configured secret and stores how long tokens should last.
- **Where it's called:** by Spring at startup, when it creates this bean; Spring fills in the two `@Value` arguments.
- **Why it's written this way:** turning the secret into a `SecretKey` once (in the constructor) means every later call reuses the same key, and keeping the secret in configuration keeps it out of the source code.
- **Step by step:**
  1. Convert the secret string to bytes (UTF-8) and build an HMAC-SHA key.
  2. Store that key in `secretKey`.
  3. Store the token lifetime in `expirationMs`.

### `public String generateToken(String userId, String email)`
```java
public String generateToken(String userId, String email) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + expirationMs);

    return Jwts.builder()
            .subject(userId)
            .claim("email", email)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(secretKey)
            .compact();
}
```
- **What it does:** creates a signed token that carries the user id (as the "subject") and the email, with issue and expiry timestamps.
- **Where it's called:** `AuthService.login` and `AuthService.verifyRegistration`, right after a user is authenticated or created.
- **Why it's written this way:** the user id is the token's subject so later requests can identify the caller without a database session; the email is added as an extra claim; signing with the secret key makes the token tamper-proof.
- **Step by step:**
  1. Record the current time and compute the expiry (`now + expirationMs`).
  2. Build a token with subject = `userId`, an `email` claim, and the two timestamps.
  3. Sign it with `secretKey` and `compact()` it into the final string.

### `public String getUserIdFromToken(String token)`
```java
public String getUserIdFromToken(String token) {
    return parseClaims(token).getSubject();
}
```
- **What it does:** returns the user id stored as the token's subject.
- **Where it's called:** `JwtAuthenticationFilter.doFilterInternal`, to learn which user is making the request.
- **Why it's written this way:** the id was stored as the subject in `generateToken`, so reading the subject back is the natural way to identify the caller.
- **Step by step:**
  1. Verify and parse the token via `parseClaims`.
  2. Return its subject.

### `public String getEmailFromToken(String token)`
```java
public String getEmailFromToken(String token) {
    return parseClaims(token).get("email", String.class);
}
```
- **What it does:** returns the email stored in the token's `email` claim.
- **Where it's called:** **not currently called** anywhere in the backend. It is provided for completeness, since the email is stored in the token and may be needed later.
- **Why it's written this way:** it mirrors `getUserIdFromToken` for the extra `email` claim, giving a typed read of that value.
- **Step by step:**
  1. Verify and parse the token via `parseClaims`.
  2. Read the `email` claim as a `String` and return it.

### `public boolean validateToken(String token)`
```java
public boolean validateToken(String token) {
    try {
        Claims claims = parseClaims(token);
        return !claims.getExpiration().before(new Date());
    } catch (Exception e) {
        return false;
    }
}
```
- **What it does:** returns `true` only if the token's signature is valid **and** it hasn't expired; any problem returns `false`.
- **Where it's called:** `JwtAuthenticationFilter.doFilterInternal`, before trusting a request's token.
- **Why it's written this way:** wrapping the check in a `try/catch` turns every failure mode (bad signature, malformed token, expired) into a simple `false`, so the filter can just skip authentication instead of crashing the request.
- **Step by step:**
  1. Parse and verify the token (throws if the signature/format is bad).
  2. Compare the expiry to now; return `true` if the expiry is not before now.
  3. If parsing threw for any reason, catch it and return `false`.
