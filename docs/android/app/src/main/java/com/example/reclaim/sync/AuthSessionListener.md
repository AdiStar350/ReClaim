```java
private AuthSessionListener() {
}
```

What that function does: I stop anyone from making an instance of this helper.
What the function is responsible for: I keep the login hook as a static method.
Where it works: I am not called. Java uses me when someone tries `new AuthSessionListener()`.
Why I chose to write the code of the function the way its written: I made the constructor private so TokenManager calls onAuthenticated.
More: I use a private constructor. The class is final.

```java
public static void onAuthenticated(@NonNull Context context) {
    PendingReportStore.adoptUnassignedAndSync(context);
}
```

What that function does: I claim guest queued reports for the new session and start a send.
What the function is responsible for: I am the post-login hook.
Where it works: I am called by TokenManager.saveToken after login, sign-up verify, or reset-then-login.
Why I chose to write the code of the function the way its written: I keep TokenManager free of Room details.
More: I call PendingReportStore.adoptUnassignedAndSync.
