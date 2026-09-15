```java
public ForgotPasswordRequest(String email) {
    this.email = email;
}
```

What that function does: I store the email that should get a reset code.
What the function is responsible for: I am the JSON body for POST /api/auth/forgot-password/send-otp.
Where it works: I am built in LoginActivity, VerifyOtpActivity, and ProfileSettingsFragment, then passed to ReClaimApiService.sendPasswordResetOtp.
Why I chose to write the code of the function the way its written: I only need email, so I kept one field and no getters.
More: I use Gson @SerializedName("email") on a final field.
