# AuthServiceTest.java

## Overview
This is a **unit test** class for `AuthService` using Mockito (see `docs/GLOSSARY.md`). It does not start Spring or MongoDB; repositories and helpers are mocked. The tests lock in two production behaviors: wrong password on login yields `401`, and forgot-password for an unknown email yields `404`.

## Concepts used here
- **Service** — `AuthService` under test; see `docs/GLOSSARY.md`.
- **Authentication / password hashing** — login must reject bad passwords; see `docs/GLOSSARY.md`.
- **HTTP status code** — asserted via `ResponseStatusException`; see `docs/GLOSSARY.md`.
- **Unit test / Mockito** — `@Mock` collaborators, `@InjectMocks` builds the service.

## Libraries and imports
- **JUnit 5** (`@Test`, assertions): test methods and `assertEquals` / `assertThrows`.
- **Mockito** (`@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`, `when`): fake `UserRepository`, `PasswordEncoder`, `JwtUtil`.
- **Spring** (`HttpStatus`, `ResponseStatusException`, `PasswordEncoder`): status codes the service throws.
- **Project**: `AuthService`, `LoginRequest`, `User`, `JwtUtil`, `UserRepository`.

## Fields
- `userRepository` / `passwordEncoder` / `jwtUtil` — mocks injected into the service.
- `authService` (`AuthService`) — real service instance under test with mocks wired in.

## Functions

### `@Test void login_rejectsWrongPassword()`
```java
@Test
void login_rejectsWrongPassword() {
    User user = User.builder()
            .id("user-1")
            .email("user@example.com")
            .password("hashed")
            .name("Test User")
            .phoneNumber("0500000000")
            .build();

    when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrong-password", "hashed")).thenReturn(false);

    ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> authService.login(new LoginRequest("user@example.com", "wrong-password")));

    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
}
```
- **What it does:** checks production login behavior: when the email exists but the password does not match, `AuthService.login` throws `401 Unauthorized`.
- **Where it's called:** by the JUnit test runner.
- **Why it's written this way:** stubs the repository and encoder so the test only exercises the match/fail branch, not real BCrypt or MongoDB.
- **Step by step:**
  1. Stub a user found by email and `matches` returning false.
  2. Call `login` with the wrong password.
  3. Assert a `ResponseStatusException` with `UNAUTHORIZED`.

### `@Test void startPasswordReset_rejectsUnknownEmail()`
```java
@Test
void startPasswordReset_rejectsUnknownEmail() {
    when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

    ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> authService.startPasswordReset("missing@example.com"));

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
}
```
- **What it does:** checks production forgot-password behavior: an email with no account causes `startPasswordReset` to throw `404 Not Found`.
- **Where it's called:** by the JUnit test runner.
- **Why it's written this way:** only the “user missing” path is stubbed; no email is actually sent.
- **Step by step:**
  1. Stub `findByEmail` to return empty.
  2. Call `startPasswordReset`.
  3. Assert status `NOT_FOUND`.
