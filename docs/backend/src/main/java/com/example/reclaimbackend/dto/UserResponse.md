```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private String id;
    private String email;
    private String name;
    private String phoneNumber;
    private String profilePictureUrl;

}
```

What that function does: I return a user profile without the password.
What the function is responsible for: I am the reply shape for profile reads and updates. I hide the hash and the FCM token.
Where it works: I am built by UserService.toResponse. UserController returns me from GET /api/users/me and PUT /api/users/me.
Why I chose to write the code of the function the way its written: I left password out on purpose so it cannot leak in JSON.
More: I use Lombok Builder, which UserService.toResponse uses to copy fields from User.
