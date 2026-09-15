```java
public String getResetToken() {
    return resetToken;
}
```

What that function does: I return the reset token from the verify-otp reply.
What the function is responsible for: I give ResetPasswordActivity the token it must send with the new password.
Where it works: I am read in VerifyOtpActivity after ReClaimApiService.verifyPasswordResetOtp succeeds.
Why I chose to write the code of the function the way its written: I only expose the token. Gson fills the field from "resetToken".
More: I use Gson @SerializedName("resetToken").
