```java
public ResendOtpRequest(String email) {
    this.email = email;
}
```

What that function does: I store the email that should get a new sign-up code.
What the function is responsible for: I am the JSON body for POST /api/auth/register/resend-otp.
Where it works: I am built in VerifyOtpActivity and passed to ReClaimApiService.resendRegistrationOtp.
Why I chose to write the code of the function the way its written: I only need email, so I kept one field.
More: I use Gson @SerializedName("email") on a final field.
