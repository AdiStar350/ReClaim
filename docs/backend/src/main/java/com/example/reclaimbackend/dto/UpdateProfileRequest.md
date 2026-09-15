```java
@Getter
@Setter
@NoArgsConstructor
public class UpdateProfileRequest {

    private String name;

    @Email(message = "Email must be valid")
    private String email;

    private String phoneNumber;

    private String profilePictureUrl;

}
```

What that function does: I carry the profile fields the signed-in user may change.
What the function is responsible for: I am the body for a profile update. Any field may be left out.
Where it works: I am read by UserController.updateCurrentUser on PUT /api/users/me. I am then passed to UserService.updateProfile.
Why I chose to write the code of the function the way its written: I made fields optional so a user can change one thing at a time. I still check email if it is sent.
More: I use Jakarta Email on email. I skipped AllArgsConstructor and Builder because the service reads fields one by one with getters.
