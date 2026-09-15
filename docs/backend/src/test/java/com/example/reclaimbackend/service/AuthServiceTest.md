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

What that function does: I check that login throws 401 when the password does not match.
What the function is responsible for: I test AuthService.login's wrong-password path.
Where it works: I call AuthService.login with a LoginRequest.
Why I chose to write the code of the function the way its written: I mock findByEmail and matches so I do not need a real database. I assert the status code, not the message text.
More: I use Mockito when, JUnit assertThrows and assertEquals, User.builder, and LoginRequest.

```java
@Test
void startPasswordReset_rejectsUnknownEmail() {
    when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

    ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> authService.startPasswordReset("missing@example.com"));

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
}
```

What that function does: I check that a password reset for an unknown email throws 404.
What the function is responsible for: I test AuthService.startPasswordReset when findByEmail is empty.
Where it works: I call AuthService.startPasswordReset.
Why I chose to write the code of the function the way its written: I wrote it this way because optional.empty is enough to hit the unknown-email branch without building a user.
More: I use Mockito when, Optional.empty, and assertThrows.
