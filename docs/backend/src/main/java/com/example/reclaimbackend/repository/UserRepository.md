```java
Optional<User> findByEmail(String email);
```

What that function does: I look up a user by email.
What the function is responsible for: I load the account used for login and password reset.
Where it works: I am called by AuthService.login, startPasswordReset, and resetPassword. AuthServiceTest mocks me.
Why I chose to write the code of the function the way its written: I wrote it this way because email is unique, so Optional is either one user or empty.
More: I rely on Spring Data, which maps findByEmail to the unique email index.

```java
boolean existsByEmail(String email);
```

What that function does: I return true if this email already has an account.
What the function is responsible for: I am the taken-email check before signup or a profile email change.
Where it works: I am called by AuthService.register, startRegistration, and verifyRegistration. UserService.updateProfile also calls me.
Why I chose to write the code of the function the way its written: I wrote it this way because exists is cheaper than loading the whole user when I only need yes or no.
More: I rely on Spring Data, which turns existsByEmail into a Mongo exists query.
