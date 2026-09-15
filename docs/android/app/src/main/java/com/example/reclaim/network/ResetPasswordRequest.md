```java
public ResetPasswordRequest(String email, String resetToken, String newPassword) {
    this.email = email;
    this.resetToken = resetToken;
    this.newPassword = newPassword;
}
```

What that function does: I store email, reset token, and the new password.
What the function is responsible for: I am the JSON body for POST /api/auth/forgot-password/reset.
Where it works: I am built in ResetPasswordActivity and passed to ReClaimApiService.resetPassword.
Why I chose to write the code of the function the way its written: I keep the three fields the reset route needs and no extra getters.
More: I use Gson @SerializedName on email, resetToken, and newPassword.
