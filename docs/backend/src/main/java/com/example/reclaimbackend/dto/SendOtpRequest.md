```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendOtpRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Name is required")
    private String name;

    private String phoneNumber;

    private String profilePictureUrl;
}
```

What that function does: I hold signup details until the email code is sent and later checked.
What the function is responsible for: I am the body for starting registration with a code. Phone and picture may be empty.
Where it works: I am read by AuthController.sendRegistrationOtp on POST /api/auth/register/send-otp. I am then passed to AuthService.startRegistration.
Why I chose to write the code of the function the way its written: I required email, password, and name, and left phone and picture optional so signup can stay simple.
More: I use Jakarta NotBlank, Email, and Size. Lombok Builder matches how AuthService copies these fields onto a pending record.
