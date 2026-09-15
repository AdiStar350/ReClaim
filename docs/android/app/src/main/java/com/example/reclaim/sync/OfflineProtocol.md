```java
private OfflineProtocol() {
}
```

What that function does: I stop anyone from making an instance of this helper.
What the function is responsible for: I keep the offline dialog as static methods.
Where it works: I am not called. Java uses me when someone tries `new OfflineProtocol()`.
Why I chose to write the code of the function the way its written: I made the constructor private so ReclaimApp calls attach.
More: I use a private constructor. The class is final.

```java
public static void attach(@NonNull Application app) {
    application = app;
    app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
        @Override
        public void onActivityResumed(@NonNull Activity activity) {
            currentActivity = activity;
        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {
            if (currentActivity == activity) {
                currentActivity = null;
            }
        }

        @Override
        public void onActivityCreated(@NonNull Activity a, @Nullable Bundle s) { }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {
            currentActivity = activity;
        }

        @Override
        public void onActivityStopped(@NonNull Activity a) { }

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity a, @NonNull Bundle o) { }

        @Override
        public void onActivityDestroyed(@NonNull Activity a) { }
    });
}
```

What that function does: I remember the Application and watch which activity is in front.
What the function is responsible for: I am app-wide setup so the offline dialog has a host activity.
Where it works: I am called by ReclaimApp.onCreate.
Why I chose to write the code of the function the way its written: Lifecycle callbacks give me the current screen without each activity registering.
More: I use Application.registerActivityLifecycleCallbacks.

```java
public void onActivityResumed(@NonNull Activity activity) {
    currentActivity = activity;
}
```

What that function does: I store the activity that just came to the front.
What the function is responsible for: I keep currentActivity up to date when a screen resumes.
Where it works: Android calls me from the callbacks registered in attach.
Why I chose to write the code of the function the way its written: Resume is the safest time to show a dialog.
More: I assign currentActivity.

```java
public void onActivityPaused(@NonNull Activity activity) {
    if (currentActivity == activity) {
        currentActivity = null;
    }
}
```

What that function does: I clear currentActivity if that same screen paused.
What the function is responsible for: I avoid showing a dialog on a paused activity.
Where it works: Android calls me from the callbacks registered in attach.
Why I chose to write the code of the function the way its written: I only clear if it is still the same instance, so a new screen can already be stored.
More: I compare with == on the Activity instance.

```java
public void onActivityCreated(@NonNull Activity a, @Nullable Bundle s) { }
```

What that function does: I do nothing on create.
What the function is responsible for: I fill the callback interface.
Where it works: Android calls me from the callbacks registered in attach.
Why I chose to write the code of the function the way its written: I only need started and resumed for a host activity.
More: I leave the body empty.

```java
public void onActivityStarted(@NonNull Activity activity) {
    currentActivity = activity;
}
```

What that function does: I store the activity that just started.
What the function is responsible for: I catch a host a bit earlier than resume.
Where it works: Android calls me from the callbacks registered in attach.
Why I chose to write the code of the function the way its written: Start can happen before resume, and a request may fail in between.
More: I assign currentActivity.

```java
public void onActivityStopped(@NonNull Activity a) { }
```

What that function does: I do nothing on stop.
What the function is responsible for: I fill the callback interface.
Where it works: Android calls me from the callbacks registered in attach.
Why I chose to write the code of the function the way its written: Pause already clears the host.
More: I leave the body empty.

```java
public void onActivitySaveInstanceState(@NonNull Activity a, @NonNull Bundle o) { }
```

What that function does: I do nothing when state is saved.
What the function is responsible for: I fill the callback interface.
Where it works: Android calls me from the callbacks registered in attach.
Why I chose to write the code of the function the way its written: The dialog does not need saved state.
More: I leave the body empty.

```java
public void onActivityDestroyed(@NonNull Activity a) { }
```

What that function does: I do nothing on destroy.
What the function is responsible for: I fill the callback interface.
Where it works: Android calls me from the callbacks registered in attach.
Why I chose to write the code of the function the way its written: Pause already clears the host.
More: I leave the body empty.

```java
static void resetOutageAck() {
    outageAcknowledged = false;
}
```

What that function does: I clear the flag that the user already picked Exit or Continue.
What the function is responsible for: I let a later outage show the dialog again.
Where it works: I am called by BackendAvailability.markReachable.
Why I chose to write the code of the function the way its written: Package-private is enough because only BackendAvailability needs me.
More: I set outageAcknowledged to false.

```java
public static void onBackendUnreachable() {
    MAIN.post(OfflineProtocol::showIfNeeded);
}
```

What that function does: I ask the main thread to show the offline dialog if needed.
What the function is responsible for: I am the hook from the HTTP interceptor.
Where it works: I am called by RetrofitClient.interceptBackendAvailability.
Why I chose to write the code of the function the way its written: OkHttp is not on the main thread, so I post to Looper.getMainLooper.
More: I use Handler MAIN and showIfNeeded.

```java
public static void showIfNeeded(@NonNull Activity activity) {
    currentActivity = activity;
    showIfNeeded();
}
```

What that function does: I set the host activity, then I try to show the dialog.
What the function is responsible for: I let a screen request the dialog with itself as host.
Where it works: I am called by DashboardActivity.
Why I chose to write the code of the function the way its written: Passing the activity covers the case where lifecycle has not stored it yet.
More: I assign currentActivity and call showIfNeeded().

```java
public static void showIfNeeded() {
    Activity activity = currentActivity;
    if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
        return;
    }
    if (activity instanceof MainActivity) {
        return;
    }
    if (outageAcknowledged) {
        return;
    }
    boolean backendDown = !BackendAvailability.isReachable();
    boolean deviceOffline = application != null && !NetworkMonitor.isOnline(application);
    if (!backendDown && !deviceOffline) {
        return;
    }
    if (activity instanceof ReportActivity) {
        outageAcknowledged = true;
        return;
    }
    if (!dialogVisible.compareAndSet(false, true)) {
        return;
    }

    new MaterialAlertDialogBuilder(activity)
            .setCancelable(false)
            .setMessage(R.string.offline_dialog_message)
            .setPositiveButton(R.string.offline_dialog_continue, (dialog, which) -> {
                outageAcknowledged = true;
                dialogVisible.set(false);
                activity.startActivity(new Intent(activity, ReportActivity.class));
            })
            .setNegativeButton(R.string.offline_dialog_exit, (dialog, which) -> {
                outageAcknowledged = true;
                dialogVisible.set(false);
                activity.finishAffinity();
            })
            .show();
}
```

What that function does: I show one offline dialog: continue to report, or exit the app.
What the function is responsible for: I am the outage UI. One dialog per outage.
Where it works: I am called from onBackendUnreachable on the main thread, and from the Activity overload.
Why I chose to write the code of the function the way its written: I skip MainActivity because it immediately forwards. I skip ReportActivity because the user is already writing. compareAndSet stops a second dialog.
More: I use MaterialAlertDialogBuilder, BackendAvailability.isReachable, NetworkMonitor.isOnline, and AtomicBoolean dialogVisible.
