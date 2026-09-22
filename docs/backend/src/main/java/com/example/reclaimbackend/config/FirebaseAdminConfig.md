# FirebaseAdminConfig.java

## Overview
This is a **Spring configuration** class that starts the **Firebase Admin** SDK once at boot (see `docs/GLOSSARY.md`). Admin credentials let the backend send **FCM** push notifications and delete files in **Firebase Storage**. If credentials are missing, the app still starts; push and storage-admin features simply stay off until `FIREBASE_SERVICE_ACCOUNT` (or application-default credentials) is available.

## Concepts used here
- **Bean / configuration** — Spring creates this class and runs `@PostConstruct`; see `docs/GLOSSARY.md`.
- **Firebase** — Storage and FCM both need Admin initialization; see `docs/GLOSSARY.md`.
- **Service account** — a JSON key file that proves this server is allowed to use Firebase Admin APIs.
- **Graceful degradation** — if init fails, the server keeps running instead of crashing.

## Libraries and imports
- **Firebase Admin** (`FirebaseApp`, `FirebaseOptions`): initializes the shared Admin app and sets the storage bucket.
- **Google Auth** (`GoogleCredentials`): loads the service-account (or application-default) credentials.
- **Spring** (`@Configuration`): marks this as a startup config class.
- **Jakarta** (`@PostConstruct`): runs `initialize` after the bean is created.
- **SLF4J** (`Logger`, `LoggerFactory`): logs success or a warning when Firebase is skipped.
- **Java I/O** (`FileInputStream`, `IOException`): reads the service-account JSON from disk.

## Fields
- `log` (`Logger`) — static logger for init success/failure messages.

## Functions

### `private GoogleCredentials loadCredentials() throws IOException`
```java
private GoogleCredentials loadCredentials() throws IOException {
    String path = System.getenv("FIREBASE_SERVICE_ACCOUNT");
    if (path != null && !path.isBlank()) {
        try (FileInputStream stream = new FileInputStream(path)) {
            return GoogleCredentials.fromStream(stream);
        }
    }
    return GoogleCredentials.getApplicationDefault();
}
```
- **What it does:** loads Google credentials from `FIREBASE_SERVICE_ACCOUNT` if set, otherwise tries application-default credentials (e.g. from the environment).
- **Where it's called:** only by `initialize` in this class.
- **Why it's written this way:** a local path is the usual student/dev setup; application-default credentials cover cloud-hosted runs without changing code.
- **Step by step:**
  1. Read the `FIREBASE_SERVICE_ACCOUNT` env var.
  2. If it is a non-blank path, open the file and build credentials from the stream.
  3. Otherwise return `GoogleCredentials.getApplicationDefault()`.

### `public void initialize()`
```java
@PostConstruct
public void initialize() {
    if (!FirebaseApp.getApps().isEmpty()) {
        return;
    }
    try {
        GoogleCredentials credentials = loadCredentials();
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setStorageBucket("reclaim-5b4fd.firebasestorage.app")
                .build();
        FirebaseApp.initializeApp(options);
        log.info("Firebase Admin initialized; push notifications enabled");
    } catch (IOException e) {
        log.warn("Firebase Admin not initialized ({}); push notifications disabled. "
                + "Set FIREBASE_SERVICE_ACCOUNT to a service-account JSON path to enable.",
                e.getMessage());
    }
}
```
- **What it does:** initializes `FirebaseApp` once with credentials and the project storage bucket, or logs a warning and leaves Firebase unused if credentials cannot be loaded.
- **Where it's called:** by Spring once after this bean is constructed (`@PostConstruct`). Later, `PushNotificationService` and `FirebaseStorageService` check `FirebaseApp.getApps()` before using Admin APIs.
- **Why it's written this way:** the empty-apps check avoids double-init; catching `IOException` lets the API server boot without Firebase (auth/items still work).
- **Step by step:**
  1. If a Firebase app already exists, return immediately.
  2. Load credentials via `loadCredentials`.
  3. Build `FirebaseOptions` with those credentials and the fixed storage bucket name.
  4. Call `FirebaseApp.initializeApp(options)` and log success.
  5. On `IOException`, log a warning that pushes are disabled.
