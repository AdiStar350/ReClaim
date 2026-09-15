```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FcmTokenRequest {

    @NotBlank(message = "FCM token is required")
    private String token;
}
```

What that function does: I carry the device token used to send this user push messages.
What the function is responsible for: I am the body for saving an FCM token. I only hold the token string.
Where it works: I am read by UserController.updateFcmToken on PUT /api/users/me/fcm-token. The controller then passes token to UserService.updateFcmToken.
Why I chose to write the code of the function the way its written: I used one required field. I did not add a builder because a single string does not need one.
More: I use Jakarta NotBlank so an empty token is rejected. Lombok Getter, Setter, NoArgsConstructor, and AllArgsConstructor fill in the rest.
