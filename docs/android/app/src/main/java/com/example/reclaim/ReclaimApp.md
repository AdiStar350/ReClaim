# ReclaimApp.java

## Overview
This is the custom **Application** class — the object Android creates once when the process starts, before any screen. It wires up notification channels, offline-report helpers, network watching, background sync scheduling, and the push-notification token registrar so those pieces are ready for the rest of the app.

## Concepts used here
- **Application** — a long-lived app-wide object (not a screen). Declared in `AndroidManifest.xml` as `android:name=".ReclaimApp"`.
- **Firebase / FCM** — push notifications; the token registrar runs at startup. See `docs/GLOSSARY.md`.
- **WorkManager (via `ReportSyncScheduler`)** — schedules reliable background sync of offline reports. See `docs/GLOSSARY.md`.

## Libraries and imports
- **Android framework**: `Application` is the base class.
- **Project classes**: `AppNotifications` creates notification channels; `OfflineProtocol` and `NetworkMonitor` watch connectivity / offline behavior; `ReportSyncScheduler` queues report sync work; `FcmTokenRegistrar` registers the device for push notifications.

## Functions

### `public void onCreate()`
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
- **What it does:** Runs one-time app startup setup: notification channels, offline protocol, network monitor, report sync job, and FCM token registration.
- **Where it's called:** By the Android system when the app process starts (because the manifest names this class as the Application).
- **Why it's written this way:** Putting shared setup here means every screen can rely on channels, sync, and push registration without each Activity repeating the work.
- **Step by step:**
  1. Call `super.onCreate()`.
  2. Create notification channels via `AppNotifications.createChannels`.
  3. Attach `OfflineProtocol` to this Application.
  4. Register `NetworkMonitor` to watch connectivity.
  5. Enqueue `ReportSyncScheduler` so offline reports can sync later.
  6. Register the FCM push token through `FcmTokenRegistrar`.
