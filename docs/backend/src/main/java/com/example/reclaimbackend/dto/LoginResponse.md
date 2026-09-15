```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse { private String token; }
```

What that function does: I return the token after a successful login or a checked signup.
What the function is responsible for: I am the reply shape for a signed-in session. I only hold token.
Where it works: I am returned by AuthService.login and AuthService.verifyRegistration. AuthController.login and AuthController.verifyRegistrationOtp send me on POST /api/auth/login and POST /api/auth/register/verify-otp.
Why I chose to write the code of the function the way its written: I wrapped the token in a field so the client can read JSON the same way as other replies.
More: I use Lombok AllArgsConstructor. AuthService builds me with new LoginResponse(token).
