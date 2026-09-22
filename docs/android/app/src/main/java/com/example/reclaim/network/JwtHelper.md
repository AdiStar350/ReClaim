# JwtHelper.java

## Overview
This file is a **singleton helper** that reads information out of a JWT (JSON Web
Token) on the phone. A JWT is a signed string made of three dot-separated parts
(`header.payload.signature`); the middle part, the payload, is Base64-encoded JSON
holding "claims" like the user id and the expiry time. See JWT in `docs/GLOSSARY.md`.
This helper only *reads* the payload — it does not verify the signature. The server is
responsible for checking that the token is genuine; the app just peeks at the claims
for convenience.

## Concepts used here
- **JWT.** The signed login token being decoded; see `docs/GLOSSARY.md`.
- **Base64 (URL-safe).** A way to store bytes as text; the token's payload is Base64
  encoded and must be decoded before reading.
- **JSON parsing.** The decoded payload is JSON, read with `org.json.JSONObject`.
- **Claims `sub` and `exp`.** `sub` is the subject (here the user id); `exp` is the
  expiry time in seconds since 1970.
- **Static utility class.** All methods are static; the constructor is private.

## Libraries and imports
- **Android (`android.util.Base64`).** Decodes the token's payload with the flags
  `URL_SAFE | NO_WRAP | NO_PADDING`, which match how JWTs encode their parts.
- **AndroidX annotations (`androidx.annotation.Nullable`).** Marks that the token
  argument and the returned values may be `null`.
- **JSON (`org.json.JSONObject`).** Parses the decoded payload; `optString`/`optLong`
  read a claim with a default if it is missing.

## Functions
Helper first, then the public readers.

### `private static JSONObject decodePayload(@Nullable String token)`
```java
@Nullable
private static JSONObject decodePayload(@Nullable String token) {
    if (token == null || token.isEmpty()) {
        return null;
    }

    String[] parts = token.split("\\.");
    if (parts.length < 2) {
        return null;
    }

    try {
        byte[] decoded = Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
        return new JSONObject(new String(decoded));
    } catch (Exception e) {
        return null;
    }
}
```
- **What it does:** turns a token string into its payload as a `JSONObject`, or
  returns `null` if the token is missing or malformed.
- **Where it's called:** inside this class by `getUserId` and `isExpired`. It is the
  shared decoder both public methods rely on.
- **Why it's written this way:** the try/catch and the many null checks make it safe
  against broken or fake tokens — instead of crashing, it returns `null` and the
  callers treat that as "no valid token."
- **Step by step:**
  1. If the token is null or empty, return `null`.
  2. Split it on `.`; a real JWT has at least two parts, so if fewer, return `null`.
  3. Base64-decode the second part (the payload) with URL-safe flags.
  4. Build a `JSONObject` from the decoded text and return it.
  5. If anything throws, return `null`.

### `public static String getUserId(@Nullable String token)`
```java
@Nullable
public static String getUserId(@Nullable String token) {
    JSONObject payload = decodePayload(token);
    return payload != null ? payload.optString("sub", null) : null;
}
```
- **What it does:** returns the user id (the `sub` claim) from the token, or `null`.
- **Where it's called:** by `TokenManager.getUserId`, which many screens use to learn
  the current user's id.
- **Why it's written this way:** the user id is already inside the token, so reading it
  locally avoids an extra network call; delegating decoding to `decodePayload` keeps
  this method short.
- **Step by step:** 1) decode the payload; 2) if that worked, read the `sub` field
  (default `null`); 3) otherwise return `null`.

### `public static boolean isExpired(@Nullable String token)`
```java
public static boolean isExpired(@Nullable String token) {
    JSONObject payload = decodePayload(token);
    if (payload == null) {
        return true;
    }

    long expSeconds = payload.optLong("exp", 0L);
    if (expSeconds <= 0L) {
        return true;
    }

    long nowSeconds = System.currentTimeMillis() / 1000L;
    return nowSeconds >= expSeconds;
}
```
- **What it does:** returns `true` if the token is missing, broken, or past its expiry
  time.
- **Where it's called:** by `TokenManager.hasToken`, which decides at startup whether
  the user is still signed in.
- **Why it's written this way:** it treats "cannot read it" and "no expiry" as expired
  on purpose — a token the app cannot trust should be considered invalid. Times are
  compared in seconds because `exp` is stored in seconds.
- **Step by step:**
  1. Decode the payload; if that fails, treat as expired (`true`).
  2. Read the `exp` claim (default `0`); if it is `0` or negative, treat as expired.
  3. Get the current time in seconds.
  4. Return `true` if now is at or past `exp`.
