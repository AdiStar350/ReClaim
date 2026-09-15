```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

}
```

What that function does: I carry email and password for a login attempt.
What the function is responsible for: I am the body for login. I do not hold extra profile fields.
Where it works: I am read by AuthController.login on POST /api/auth/login. I am then passed to AuthService.login. AuthServiceTest builds me with new LoginRequest(...).
Why I chose to write the code of the function the way its written: I kept two fields and checked they are not blank so empty logins fail before the service.
More: I use Jakarta NotBlank and Email. Lombok AllArgsConstructor lets the test build me in one line.
