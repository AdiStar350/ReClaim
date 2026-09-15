```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Name is required")
    private String name;

    private String phoneNumber;

}
```

What that function does: I carry the details used to create an account without the email-code step.
What the function is responsible for: I am the body for POST /api/auth/register. Phone may be empty.
Where it works: I am read by AuthController.register. I am then passed to AuthService.register.
Why I chose to write the code of the function the way its written: I required email, password, and name, and left phone optional. I did not include a picture field here.
More: I use Jakarta NotBlank, Email, and Size. AuthService.register copies these fields onto a User after hashing the password.
