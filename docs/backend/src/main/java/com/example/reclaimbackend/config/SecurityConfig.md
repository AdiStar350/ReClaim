```java
@Bean
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

What that function does: I set who can call which routes: auth is public, everything else needs a token.
What the function is responsible for: I am the securityFilterChain bean. I wire the JWT filter in.
Where it works: I am used by Spring Security for every HTTP request. ItemControllerSecurityTest hits POST /api/items without a token and expects it to be blocked.
Why I chose to write the code of the function the way its written: I turned CSRF off because we use tokens, not cookies. I listed the public auth paths so signup still works.
More: I use HttpSecurity, SessionCreationPolicy.STATELESS, and addFilterBefore with JwtAuthenticationFilter.

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

What that function does: I give the app a BCrypt encoder to hash and check passwords.
What the function is responsible for: I am the PasswordEncoder bean.
Where it works: I am injected by Spring into AuthService and UserService. I am unused except as a bean.
Why I chose to write the code of the function the way its written: I wrote it this way because bCrypt is the usual Spring choice for password hashes. I did not wrap it further.
More: I use Spring Security BCryptPasswordEncoder.

```java
@Bean
public AuthenticationManager authenticationManager(
        AuthenticationConfiguration authenticationConfiguration) throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
}
```

What that function does: I expose Spring's AuthenticationManager as a bean.
What the function is responsible for: I am the authenticationManager bean Spring Security may use to sign people in.
Where it works: I can be injected by Spring. I did not find Java callers in this backend; login uses AuthService and JwtUtil instead.
Why I chose to write the code of the function the way its written: I returned the manager from AuthenticationConfiguration, which is the usual Spring Boot hook.
More: I use AuthenticationConfiguration.getAuthenticationManager.
