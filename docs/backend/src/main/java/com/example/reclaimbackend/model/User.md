```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String password;

    private String name;

    private String phoneNumber;

    /** Device token for push notifications. May be empty. */
    private String fcmToken;

    /** Profile picture URL. May be empty. */
    private String profilePictureUrl;

}
```

What that function does: I store a user: email, hashed password, name, phone, picture, and optional FCM token.
What the function is responsible for: I am the Mongo document in users. Email is unique so two accounts cannot share it.
Where it works: I am created by AuthService on signup and loaded on login. UserService updates and deletes me. ClaimService and ItemService load me for contact and pushes. Tests build me with User.builder().
Why I chose to write the code of the function the way its written: I keep password on the document but services strip or hide it when sending JSON. fcmToken stays here so pushes know the device.
More: I use Spring Data Document, Id, and Indexed. Lombok Builder is used in AuthService and tests.
