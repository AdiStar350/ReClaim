```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "password_reset_otps")
public class PasswordResetToken {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    /** Hashed 6-digit code. */
    private String otpHash;

    /** How many wrong codes have been tried. */
    private int attempts;

    /** True after the code was accepted. */
    private boolean verified;

    /** Random token given after the code is accepted. May be empty. */
    private String resetToken;

    private Instant createdAt;

    private Instant expiresAt;
}
```

What that function does: I store one forgot-password record: hashed code, tries, and later a reset token.
What the function is responsible for: I am the Mongo document in password_reset_otps. Email is unique so each user has at most one open reset.
Where it works: I am saved and read by AuthService.startPasswordReset, verifyPasswordResetOtp, and resetPassword through PasswordResetTokenRepository.
Why I chose to write the code of the function the way its written: I hash the code and keep attempts so a guessed code is limited. I add resetToken only after the code is accepted.
More: I use Spring Data Document, Id, and Indexed. Instant marks when I was made and when I expire. Lombok Builder is how AuthService creates me.
