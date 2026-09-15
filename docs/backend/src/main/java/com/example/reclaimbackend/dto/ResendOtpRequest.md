```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResendOtpRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;
}
```

What that function does: I carry the email that needs a new signup code.
What the function is responsible for: I am the body for resending a registration code. I only hold email.
Where it works: I am read by AuthController.resendRegistrationOtp on POST /api/auth/register/resend-otp. The controller then passes the email to AuthService.resendOtp.
Why I chose to write the code of the function the way its written: I kept one field because a resend only needs to know who is waiting.
More: I use Jakarta NotBlank and Email. Lombok gives me getters so the controller can call getEmail().
