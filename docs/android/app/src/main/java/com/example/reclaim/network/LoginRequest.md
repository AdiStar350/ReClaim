```java
public LoginRequest(String email, String password) {
    this.email = email;
    this.password = password;
}
```

What that function does: I store email and password for a login call.
What the function is responsible for: I am the JSON body for POST /api/auth/login.
Where it works: I am built in LoginActivity and ResetPasswordActivity, then passed to ReClaimApiService.login.
Why I chose to write the code of the function the way its written: I used final fields so the body cannot change after I am made.
More: I use Gson @SerializedName on email and password.

```java
public String getEmail() {
    return email;
}
```

What that function does: I return the email I was built with.
What the function is responsible for: I let callers read the email if they need it.
Where it works: Gson reads the field by name. I did not find an app caller of getEmail on this class.
Why I chose to write the code of the function the way its written: I kept a simple getter next to the field.
More: I return the final email field.

```java
public String getPassword() {
    return password;
}
```

What that function does: I return the password I was built with.
What the function is responsible for: I let callers read the password if they need it.
Where it works: Gson reads the field by name. I did not find an app caller of getPassword on this class.
Why I chose to write the code of the function the way its written: I kept a simple getter next to the field.
More: I return the final password field.
