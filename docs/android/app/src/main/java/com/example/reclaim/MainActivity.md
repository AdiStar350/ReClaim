```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent intent;
    if (TokenManager.hasToken(this)) {
        intent = new Intent(this, DashboardActivity.class);
    } else {
        intent = new Intent(this, LoginActivity.class);
    }

    startActivity(intent);
    finish();
}
```

What that function does: I send a signed-in user to the dashboard, or everyone else to login, then I close myself.
What the function is responsible for: I am the first screen. I only choose the next screen.
Where it works: I am the launcher activity in AndroidManifest. OfflineProtocol skips its dialog while I am showing.
Why I chose to write the code of the function the way its written: I do not set a layout because I never stay on screen. finish() keeps me off the back stack.
More: I use TokenManager.hasToken and Intent to start DashboardActivity or LoginActivity.
