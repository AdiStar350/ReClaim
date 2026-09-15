```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetTokenResponse {
    private String resetToken;
}
```

What that function does: I return the token used to set a new password after the reset code is accepted.
What the function is responsible for: I am the reply shape for a checked reset code. I only hold resetToken.
Where it works: I am built by AuthController.verifyPasswordResetOtp on POST /api/auth/forgot-password/verify-otp after AuthService.verifyPasswordResetOtp returns the token.
Why I chose to write the code of the function the way its written: I wrapped the token in a named field so the client does not have to parse a raw string.
More: I use Lombok Getter, Setter, NoArgsConstructor, AllArgsConstructor, and Builder. AuthController uses the all-args constructor.
