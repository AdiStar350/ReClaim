```java
@Override
public void onCreate() {
    super.onCreate();
    AppNotifications.createChannels(this);
    OfflineProtocol.attach(this);
    NetworkMonitor.register(this);
    ReportSyncScheduler.enqueue(this);
    FcmTokenRegistrar.register(this);
}
```

What that function does: I start notice channels, offline watch, network watch, queued report send, and the push token.
What the function is responsible for: I am app start setup. Screens do not have to do this themselves.
Where it works: I am named in AndroidManifest as the Application. I run once when the process starts.
Why I chose to write the code of the function the way its written: I keep one place for work that must exist before any screen.
More: I call AppNotifications.createChannels, OfflineProtocol.attach, NetworkMonitor.register, ReportSyncScheduler.enqueue, and FcmTokenRegistrar.register.
