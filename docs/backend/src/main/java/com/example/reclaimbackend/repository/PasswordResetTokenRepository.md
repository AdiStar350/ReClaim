```java
Optional<PasswordResetToken> findByEmail(String email);
```

What that function does: I look up the password-reset record for one email.
What the function is responsible for: I am the query Spring Data runs against password_reset_otps by email.
Where it works: I am called by AuthService.verifyPasswordResetOtp and AuthService.resetPassword.
Why I chose to write the code of the function the way its written: I return Optional so a missing email is an empty result, not null.
More: I extend MongoRepository. Spring Data builds the query from the method name findByEmail.

```java
void deleteByEmail(String email);
```

What that function does: I delete the password-reset record for one email.
What the function is responsible for: I clear any leftover reset so a new code can be stored.
Where it works: I am called by AuthService.startPasswordReset before saving a new record.
Why I chose to write the code of the function the way its written: I delete by email because email is unique on the document.
More: I rely on Spring Data, which turns deleteByEmail into a Mongo delete. I inherit save and findById from MongoRepository for the rest of AuthService.
