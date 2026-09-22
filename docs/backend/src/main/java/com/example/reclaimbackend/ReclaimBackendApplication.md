# ReclaimBackendApplication.java

## Overview
This is the **Spring Boot entry point** for the ReClaim backend (see `docs/GLOSSARY.md`). Running `main` boots the whole server: it scans this package for controllers, services, configs, and repositories, then starts the embedded web server that serves the HTTP API the Android app calls.

## Concepts used here
- **Spring Boot** — the framework that wires beans and runs the web server; see `docs/GLOSSARY.md`.
- **Application entry point** — `main` is what the JVM starts when you launch the backend.
- **Annotation** — `@SpringBootApplication` turns on auto-configuration and component scanning; see `docs/GLOSSARY.md`.
- **Client and server** — this process is the server side of ReClaim; see `docs/GLOSSARY.md`.

## Libraries and imports
- **Spring Boot** (`SpringApplication`, `@SpringBootApplication`): `SpringApplication.run(...)` starts the context; `@SpringBootApplication` enables auto-config and scans `com.example.reclaimbackend` for beans.

## Fields
*(none.)*

## Functions

### `public static void main(String[] args)`
```java
public static void main(String[] args) {
    SpringApplication.run(ReclaimBackendApplication.class, args);
}
```
- **What it does:** starts the Spring Boot application using this class as the primary source, passing through any command-line arguments.
- **Where it's called:** by the JVM when the backend process starts (IDE run, `mvn spring-boot:run`, or a packaged jar).
- **Why it's written this way:** this is the standard Spring Boot bootstrap; one line hands control to the framework, which then creates all `@Configuration`, `@Service`, and `@RestController` beans.
- **Step by step:**
  1. Pass this class and `args` to `SpringApplication.run`.
  2. Spring Boot builds the application context and starts the web server.
  3. The process stays alive serving API requests until shut down.
