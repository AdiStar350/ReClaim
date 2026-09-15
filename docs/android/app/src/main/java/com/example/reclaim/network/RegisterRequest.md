```java
public RegisterRequest(String email, String password,
                       String name, String phoneNumber) {
    this.email = email;
    this.password = password;
    this.name = name;
    this.phoneNumber = phoneNumber;
}
```

What that function does: I store email, password, name, and phone for direct sign-up.
What the function is responsible for: I am the JSON body for POST /api/auth/register.
Where it works: ReClaimApiService.register takes me. I did not find a screen that builds me; sign-up uses SendOtpRequest instead.
Why I chose to write the code of the function the way its written: I kept the four fields the old register route needs.
More: I use Gson @SerializedName on each field.

```java
public String getEmail() {
    return email;
}
```

What that function does: I return the email I was built with.
What the function is responsible for: I let callers read the email.
Where it works: I did not find an app caller of this getter. Gson reads the field.
Why I chose to write the code of the function the way its written: I kept a simple getter next to the field.
More: I return the final email field.

```java
public String getPassword() {
    return password;
}
```

What that function does: I return the password I was built with.
What the function is responsible for: I let callers read the password.
Where it works: I did not find an app caller of this getter. Gson reads the field.
Why I chose to write the code of the function the way its written: I kept a simple getter next to the field.
More: I return the final password field.

```java
public String getName() {
    return name;
}
```

What that function does: I return the name I was built with.
What the function is responsible for: I let callers read the name.
Where it works: I did not find an app caller of this getter. Gson reads the field.
Why I chose to write the code of the function the way its written: I kept a simple getter next to the field.
More: I return the final name field.

```java
public String getPhoneNumber() {
    return phoneNumber;
}
```

What that function does: I return the phone I was built with.
What the function is responsible for: I let callers read the phone.
Where it works: I did not find an app caller of this getter. Gson reads the field.
Why I chose to write the code of the function the way its written: I kept a simple getter next to the field.
More: I return the final phoneNumber field.
