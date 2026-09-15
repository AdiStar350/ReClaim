```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyOtpRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Verification code is required")
    @Pattern(regexp = "\\d{6}", message = "Verification code must be 6 digits")
    private String code;
}
```

What that function does: I carry an email and a 6-digit code to check.
What the function is responsible for: I am the body for checking a signup or password-reset code.
Where it works: I am read by AuthController.verifyRegistrationOtp and AuthController.verifyPasswordResetOtp on POST /api/auth/register/verify-otp and POST /api/auth/forgot-password/verify-otp, then pass me to AuthService.
Why I chose to write the code of the function the way its written: I reused one shape for both flows. The pattern makes sure the code is six digits.
More: I use Jakarta NotBlank, Email, and Pattern. AuthService.verifyRegistration and AuthService.verifyPasswordResetOtp both take me.
