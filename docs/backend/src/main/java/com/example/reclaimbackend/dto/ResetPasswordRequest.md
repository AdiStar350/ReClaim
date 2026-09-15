```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Reset token is required")
    private String resetToken;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String newPassword;
}
```

What that function does: I carry email, the reset token, and the new password so the account can be updated.
What the function is responsible for: I am the body for finishing a password reset. I keep the three values together.
Where it works: I am read by AuthController.resetPassword on POST /api/auth/forgot-password/reset. I am then passed to AuthService.resetPassword.
Why I chose to write the code of the function the way its written: I checked each field so a short password or missing token is stopped early.
More: I use Jakarta NotBlank, Email, and Size. Lombok Builder lets tests or callers build me if needed.
