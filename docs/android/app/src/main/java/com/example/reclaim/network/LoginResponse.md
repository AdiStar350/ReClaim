```java
public LoginResponse() {
}
```

What that function does: I make an empty reply so Gson can fill the token later.
What the function is responsible for: I am the no-arg constructor Gson needs.
Where it works: Gson uses me when ReClaimApiService.login or verifyRegistrationOtp parse JSON.
Why I chose to write the code of the function the way its written: Gson needs an empty constructor to create me before setToken.
More: I leave token null until Gson sets it.

```java
public LoginResponse(String token) {
    this.token = token;
}
```

What that function does: I hold a login token in one step.
What the function is responsible for: I let tests or code build a reply without JSON.
Where it works: I did not find an app caller of this constructor. Gson uses the empty one.
Why I chose to write the code of the function the way its written: I kept a one-field constructor in case a caller wants to build me by hand.
More: I set the token field.

```java
public String getToken() {
    return token;
}
```

What that function does: I return the token from the login reply.
What the function is responsible for: I give the token to TokenManager.saveToken.
Where it works: I am read in LoginActivity and VerifyOtpActivity after a successful login or sign-up verify.
Why I chose to write the code of the function the way its written: I kept one getter so screens do not touch the field.
More: I return the token Gson mapped from "token".

```java
public void setToken(String token) {
    this.token = token;
}
```

What that function does: I set the token field.
What the function is responsible for: I let Gson write the token after the empty constructor.
Where it works: Gson may call me while parsing LoginResponse JSON.
Why I chose to write the code of the function the way its written: I kept a setter because token is not final.
More: I assign the token field.
