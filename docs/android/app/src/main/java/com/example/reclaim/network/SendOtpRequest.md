```java
public SendOtpRequest(String email, String password, String name,
                      String phoneNumber, String profilePictureUrl) {
    this.email = email;
    this.password = password;
    this.name = name;
    this.phoneNumber = phoneNumber;
    this.profilePictureUrl = profilePictureUrl;
}
```

What that function does: I store sign-up fields, including an optional picture URL, before the account exists.
What the function is responsible for: I am the JSON body for POST /api/auth/register/send-otp.
Where it works: I am built in RegisterActivity and passed to ReClaimApiService.sendRegistrationOtp.
Why I chose to write the code of the function the way its written: I send the picture URL with the rest so the server can keep it on the pending sign-up.
More: I use Gson @SerializedName on email, password, name, phoneNumber, and profilePictureUrl.
