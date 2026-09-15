```java
@Getter
@Setter
public class DeleteAccountRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String password;
}
```

What that function does: I carry the name and password needed to delete an account.
What the function is responsible for: I am the body for account deletion. Both fields must be present.
Where it works: I am read by UserController.deleteCurrentUser on DELETE /api/users/me. The controller then passes name and password to UserService.deleteAccount.
Why I chose to write the code of the function the way its written: I asked for name and password so a delete is not a single tap. I kept the class small.
More: I use Jakarta NotBlank. I only used Getter and Setter because Spring can fill me from JSON without extra constructors.
