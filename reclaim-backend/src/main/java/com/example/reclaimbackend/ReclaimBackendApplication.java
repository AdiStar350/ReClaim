package com.example.reclaimbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the ReClaim backend application.
 * <p>
 * A Spring Boot 3 REST API powering the ReClaim Lost &amp; Found platform.
 * Uses MongoDB Atlas for data persistence, Spring Security with JWT for
 * authentication, and Firebase Storage for image hosting.
 * </p>
 */
@SpringBootApplication
public class ReclaimBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReclaimBackendApplication.class, args);
    }
}
