# SecurityConfig.java

## Overview
This is a **Configuration** class: a class that sets up how the app behaves rather than handling requests itself. A "configuration" defines beans and rules Spring wires in at startup. `SecurityConfig` decides who may call which routes (login/signup are public, everything else needs a token), turns off session cookies in favor of tokens, and provides the password encoder and authentication manager. See `docs/GLOSSARY.md` for authentication, JWT, and beans.

## Concepts used here
- **Configuration class** — a class marked `@Configuration` that defines beans and settings; see `docs/GLOSSARY.md` (Spring Boot building blocks).
- **Bean** — an object Spring creates and manages; the `@Bean` methods here each produce one; see `docs/GLOSSARY.md`.
- **Stateless / no session** — the server keeps no per-user session; each request must carry its own token.
- **Filter chain** — the ordered list of checkpoints a request passes through.
- **Password hashing (BCrypt)** — one-way password scrambling; see `docs/GLOSSARY.md`.
- **CSRF** — a cookie-based attack that doesn't apply to a token API, so it's disabled here.

## Libraries and imports
- **Spring Security config** (`HttpSecurity`, `EnableWebSecurity`, `SessionCreationPolicy`, `SecurityFilterChain`, `AuthenticationConfiguration`, `AuthenticationManager`, `UsernamePasswordAuthenticationFilter`): the toolkit for describing security rules. `HttpSecurity` is the builder for the rules; `@EnableWebSecurity` turns Spring Security on; `SessionCreationPolicy.STATELESS` disables sessions; `SecurityFilterChain` is the built rule set; `AuthenticationConfiguration`/`AuthenticationManager` are Spring's sign-in machinery; `UsernamePasswordAuthenticationFilter` is the built-in filter our JWT filter is placed *before*.
- **Spring Security crypto** (`BCryptPasswordEncoder`, `PasswordEncoder`): `PasswordEncoder` is the interface for hashing/checking passwords; `BCryptPasswordEncoder` is the BCrypt implementation used everywhere passwords are hashed or compared.
- **Spring** (`@Configuration`, `@Bean`): `@Configuration` marks the class; `@Bean` marks methods whose return value becomes a managed object.
- **Lombok** (`@RequiredArgsConstructor`): generates a constructor for the `final` filter field so Spring injects it.
- **Project class** (`JwtAuthenticationFilter`): the custom filter added to the chain.

## Fields
- `jwtAuthenticationFilter` (`JwtAuthenticationFilter`) — the token-reading filter, injected by Spring and inserted into the security chain.

## Functions

### `public SecurityFilterChain securityFilterChain(HttpSecurity http)`
```java
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
            // No CSRF. We use tokens, not cookies.
            .csrf(csrf -> csrf.disable())

            // No server-side session.
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Who can call what
            .authorizeHttpRequests(auth -> auth
                    // Login, signup, and password reset are open
                    .requestMatchers("/api/auth/login").permitAll()
                    .requestMatchers("/api/auth/register").permitAll()
                    .requestMatchers("/api/auth/register/**").permitAll()
                    .requestMatchers("/api/auth/forgot-password/**").permitAll()
                    // Everything else needs a signed-in user
                    .anyRequest().authenticated()
            )

            // Check the token before the usual login filter
            .addFilterBefore(jwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class);

    return http.build();
}
```
- **What it does:** builds the security rules: disable CSRF, use no session, allow the auth routes to everyone, require a logged-in user for all other routes, and run the JWT filter early in the chain.
- **Where it's called:** by Spring at startup; the returned `SecurityFilterChain` bean is what actually governs requests. (`@Bean`)
- **Why it's written this way:** the API uses tokens, not cookies, so sessions and CSRF protection aren't needed (and would get in the way). Only login/signup/reset can be public — you must be able to reach them *before* you have a token — while `anyRequest().authenticated()` locks down everything else by default (safer than listing protected routes one by one). Placing the JWT filter before `UsernamePasswordAuthenticationFilter` means the user id is set from the token before Spring's normal login logic runs.
- **Step by step:**
  1. Disable CSRF (not relevant for a stateless token API).
  2. Set session creation to `STATELESS` so no session is stored.
  3. Permit the login, register (and sub-paths), and forgot-password (sub-paths) routes for everyone.
  4. Require authentication for every other request.
  5. Insert `jwtAuthenticationFilter` before the built-in username/password filter.
  6. Build and return the filter chain.

### `public PasswordEncoder passwordEncoder()`
```java
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```
- **What it does:** provides the shared object used to hash new passwords and to check a typed password against a stored hash.
- **Where it's called:** by Spring at startup to create the bean; it is then injected into `AuthService` and `UserService`, which call `encode(...)` and `matches(...)` on it. (`@Bean`)
- **Why it's written this way:** BCrypt is a slow, salted hashing scheme designed for passwords, so it resists guessing; defining it once as a bean means the whole app hashes and checks passwords the same way.
- **Step by step:**
  1. Create a `BCryptPasswordEncoder`.
  2. Return it so Spring can inject it wherever a `PasswordEncoder` is needed.

### `public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)`
```java
public AuthenticationManager authenticationManager(
        AuthenticationConfiguration authenticationConfiguration) throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
}
```
- **What it does:** exposes Spring Security's `AuthenticationManager` as a bean by pulling it from Spring's auth configuration.
- **Where it's called:** by Spring at startup to create the bean. (`@Bean`)
- **Why it's written this way:** exposing the manager as a bean makes it available for injection if authentication logic needs it; getting it from `AuthenticationConfiguration` reuses Spring's own setup instead of building one by hand.
- **Step by step:**
  1. Receive Spring's `AuthenticationConfiguration`.
  2. Ask it for the built `AuthenticationManager` and return it.
