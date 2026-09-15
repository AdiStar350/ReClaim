```java
Optional<PendingRegistration> findByEmail(String email);
```

What that function does: I look up the pending signup for one email.
What the function is responsible for: I load the row AuthService needs to resend or check a code.
Where it works: I am called by AuthService.resendOtp and AuthService.verifyRegistration.
Why I chose to write the code of the function the way its written: I return Optional so a missing pending signup is empty, not null.
More: I rely on Spring Data, which builds findByEmail on the unique email index.

```java
void deleteByEmail(String email);
```

What that function does: I delete the pending signup for one email.
What the function is responsible for: I clear an old pending row before a new code is stored.
Where it works: I am called by AuthService.startRegistration.
Why I chose to write the code of the function the way its written: I wrote it this way because email is unique, so delete by email is enough.
More: I rely on Spring Data, which turns this into a Mongo delete. AuthService.verifyRegistration also deletes by document through delete(pending) from MongoRepository.
