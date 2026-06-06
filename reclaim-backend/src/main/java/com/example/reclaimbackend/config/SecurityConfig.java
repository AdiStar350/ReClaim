package com.example.reclaimbackend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 6 configuration for the ReClaim backend.
 * <p>
 * Configures a stateless (JWT-based) security setup:
 * <ul>
 *   <li>CSRF is disabled (stateless REST API — no cookies).</li>
 *   <li>Session management is set to {@code STATELESS}.</li>
 *   <li>The {@code /api/auth/login} and {@code /api/auth/register}
 *       endpoints are publicly accessible.</li>
 *   <li>All other endpoints require a valid JWT token.</li>
 *   <li>The {@link JwtAuthenticationFilter} runs before Spring's
 *       default {@link UsernamePasswordAuthenticationFilter}.</li>
 * </ul>
 * </p>
 *
 * <h3>Exposed Beans:</h3>
 * <ul>
 *   <li>{@link PasswordEncoder} — BCrypt for hashing user passwords.</li>
 *   <li>{@link AuthenticationManager} — required by Spring Security
 *       for programmatic authentication.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Defines the HTTP security filter chain.
     * <p>
     * Public endpoints: {@code POST /api/auth/login},
     * {@code POST /api/auth/register}.<br>
     * All other requests: authenticated via JWT.
     * </p>
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF — we use JWT tokens, not cookies
                .csrf(csrf -> csrf.disable())

                // Stateless session — no server-side session storage
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Endpoint access rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints (login + register)
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/register").permitAll()
                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )

                // Add JWT filter before the default auth filter
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCrypt password encoder bean for hashing and verifying passwords.
     * <p>
     * Used by {@link com.example.reclaimbackend.service.AuthService}
     * during registration (encode) and login (matches).
     * </p>
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Exposes the {@link AuthenticationManager} bean from Spring Security's
     * {@link AuthenticationConfiguration}.
     * <p>
     * Required for programmatic authentication in the service layer.
     * </p>
     *
     * @param authenticationConfiguration the auto-configured authentication config
     * @return the authentication manager
     * @throws Exception if the manager cannot be created
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
