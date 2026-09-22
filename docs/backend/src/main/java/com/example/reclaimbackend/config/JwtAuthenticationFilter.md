# JwtAuthenticationFilter.java

## Overview
This is a **security filter**: code that runs on **every** incoming request before it reaches a controller. A "filter" is a checkpoint in the request pipeline. This one reads the login token from the request header and, if the token is valid, tells Spring Security who the user is. It does not block anything by itself — `SecurityConfig` decides which routes require a logged-in user; this filter just supplies the identity when a good token is present. See `docs/GLOSSARY.md` for JWT and authentication.

## Concepts used here
- **JWT** — the signed login token read from the header; see `docs/GLOSSARY.md`.
- **Filter (OncePerRequestFilter)** — a checkpoint that runs once per request before controllers.
- **Spring Security context** — a per-request holder for "who is the current user."
- **Bearer token** — a token sent in the header as `Authorization: Bearer <token>`.
- **Dependency injection** — `JwtUtil` is provided through the constructor by Lombok's `@RequiredArgsConstructor`; see `docs/GLOSSARY.md`.

## Libraries and imports
- **Spring Security** (`UsernamePasswordAuthenticationToken`, `SecurityContextHolder`): `UsernamePasswordAuthenticationToken` is Spring's object for "an authenticated caller"; `SecurityContextHolder.getContext().setAuthentication(...)` stores it for the rest of the request.
- **Spring web filter** (`org.springframework.web.filter.OncePerRequestFilter`): the base class that guarantees the filter runs exactly once per request; this class overrides its `doFilterInternal`.
- **Jakarta Servlet** (`FilterChain`, `HttpServletRequest`, `HttpServletResponse`, `ServletException`): the low-level request/response types and the chain used to pass control to the next filter.
- **Spring util & Java standard** (`org.springframework.util.StringUtils`, `java.util.Collections`, `java.io.IOException`): `StringUtils.hasText` checks a header isn't blank; `Collections.emptyList()` supplies "no roles"; `IOException` is declared by the servlet API.
- **Lombok** (`@RequiredArgsConstructor`): generates a constructor for the `final` `jwtUtil` field so Spring can inject it.
- **Spring** (`@Component`): makes this a managed bean, which `SecurityConfig` inserts into the filter chain.

## Fields
- `jwtUtil` (`JwtUtil`) — the helper used to validate a token and read the user id from it.

## Functions

### `private String extractTokenFromHeader(HttpServletRequest request)`
```java
private String extractTokenFromHeader(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
        return bearerToken.substring(7);
    }
    return null;
}
```
- **What it does:** pulls the raw token out of the `Authorization` header, or returns `null` if the header is missing or not a Bearer token.
- **Where it's called:** inside this class only — by `doFilterInternal`.
- **Why it's written this way:** the standard is `Authorization: Bearer <token>`, so it checks for that exact prefix and strips the 7 characters of `"Bearer "` to leave just the token; anything else is treated as "no token."
- **Step by step:**
  1. Read the `Authorization` header.
  2. If it has text and starts with `"Bearer "`, return everything after those 7 characters.
  3. Otherwise return `null`.

### `protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)`
```java
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain filterChain)
        throws ServletException, IOException {

    String token = extractTokenFromHeader(request);

    if (token != null && jwtUtil.validateToken(token)) {
        String userId = jwtUtil.getUserIdFromToken(token);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,       // the user id
                        null,         // no password here
                        Collections.emptyList()  // no roles
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    filterChain.doFilter(request, response);
}
```
- **What it does:** on each request, if there's a valid token, records the user id as the authenticated caller for this request; then always lets the request continue down the chain.
- **Where it's called:** by the framework (Spring Security) for every request. `SecurityConfig` places this filter before the standard username/password filter. You do not call it yourself.
- **Why it's written this way:** the filter only *sets* identity and never rejects requests — that keeps one job here (read the token) and leaves "which routes need a login" to `SecurityConfig`. Storing the user id (with no password and no roles) is enough, because controllers later read it via `authentication.getName()`. If the token is missing/invalid, it simply doesn't set anyone, and protected routes will then be refused downstream.
- **Step by step:**
  1. Extract the token from the header.
  2. If a token exists and `jwtUtil.validateToken` says it's good, read the user id from it.
  3. Build an authentication object holding that user id (no password, no roles) and store it in the security context.
  4. Whether or not a user was set, call `filterChain.doFilter` to pass the request to the next step.
