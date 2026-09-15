```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForgotPasswordRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;
}
```

What that function does: I carry the email that wants a password-reset code.
What the function is responsible for: I am the body for starting a password reset. I only ask for email.
Where it works: I am read by AuthController.sendPasswordResetOtp on POST /api/auth/forgot-password/send-otp. The controller then passes the email to AuthService.startPasswordReset.
Why I chose to write the code of the function the way its written: I kept one field and put checks on it so a blank or bad email is rejected before the service runs.
More: I use Jakarta NotBlank and Email so Spring validation can stop bad input. Lombok gives me getters and constructors.
