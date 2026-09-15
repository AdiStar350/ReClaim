```java
private ConnectionToasts() {
}
```

What that function does: I stop anyone from making an instance of this helper.
What the function is responsible for: I keep this class as static methods only.
Where it works: I am not called. Java uses me when someone tries `new ConnectionToasts()`.
Why I chose to write the code of the function the way its written: I made the constructor private so callers use the static toast helpers.
More: I use a private constructor. The class is final.

```java
public static void showIfOnline(@NonNull Context context, @StringRes int resId, int duration) {
    if (NetworkMonitor.isBackendAvailable(context)) {
        Toast.makeText(context, resId, duration).show();
    }
}
```

What that function does: I show a string-resource toast only when the API is up.
What the function is responsible for: I hide error toasts while the offline dialog is the right message.
Where it works: I am called from LoginActivity, RegisterActivity, VerifyOtpActivity, ResetPasswordActivity, ReportActivity, ItemDetailsActivity, PendingClaimsActivity, EditProfileActivity, ProfileSettingsFragment, MyItemsFragment, SearchFragment, and HomeFragment.
Why I chose to write the code of the function the way its written: I check NetworkMonitor first so a dead server does not also show a toast.
More: I use NetworkMonitor.isBackendAvailable, Toast.makeText, and @StringRes for the text.

```java
public static void showIfOnline(@NonNull Context context, @NonNull CharSequence message, int duration) {
    if (NetworkMonitor.isBackendAvailable(context)) {
        Toast.makeText(context, message, duration).show();
    }
}
```

What that function does: I show a plain-text toast only when the API is up.
What the function is responsible for: I am the same gate as the resource overload, for messages built at run time.
Where it works: I am called from LoginActivity, VerifyOtpActivity, SearchFragment, and HomeFragment with a CharSequence message.
Why I chose to write the code of the function the way its written: I kept the same online check so both overloads behave the same.
More: I use NetworkMonitor.isBackendAvailable and Toast.makeText with a CharSequence.
