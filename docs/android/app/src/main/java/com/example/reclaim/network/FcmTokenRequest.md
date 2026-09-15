```java
public FcmTokenRequest(String token) {
    this.token = token;
}
```

What that function does: I store this device's push token.
What the function is responsible for: I am the JSON body for PUT /api/users/me/fcm-token.
Where it works: I am built in FcmTokenRegistrar and passed to ReClaimApiService.updateFcmToken.
Why I chose to write the code of the function the way its written: I only need the token string.
More: I use Gson @SerializedName("token") on a final field.
