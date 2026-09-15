```java
public VerifyOtpRequest(String email, String code) {
    this.email = email;
    this.code = code;
}
```

What that function does: I store the email and the 6-digit code to check.
What the function is responsible for: I am the JSON body for register verify-otp and password-reset verify-otp.
Where it works: I am built in VerifyOtpActivity and passed to ReClaimApiService.verifyRegistrationOtp and verifyPasswordResetOtp.
Why I chose to write the code of the function the way its written: Both routes take the same two fields, so I reuse one class.
More: I use Gson @SerializedName on email and code.
