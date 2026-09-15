```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "pending_registrations")
public class PendingRegistration {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    /** Hashed password. Copied onto the user later. */
    private String passwordHash;

    private String name;

    private String phoneNumber;

    /** Profile picture URL. May be empty. */
    private String profilePictureUrl;

    /** Hashed 6-digit code. */
    private String otpHash;

    /** How many wrong codes have been tried. */
    private int attempts;

    private Instant createdAt;

    private Instant expiresAt;
}
```

What that function does: I hold signup details and a hashed code until the email is checked, then I am deleted.
What the function is responsible for: I am the Mongo document in pending_registrations. I am not a User yet.
Where it works: I am saved, updated, and deleted by AuthService.startRegistration, resendOtp, and verifyRegistration through PendingRegistrationRepository.
Why I chose to write the code of the function the way its written: I hash the password and the code so a leaked pending row is less useful. Email is unique so a new signup replaces the old one.
More: I use Spring Data Document, Id, and Indexed. Instant marks createdAt and expiresAt. Lombok Builder is how AuthService creates me.
