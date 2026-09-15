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

What that function does: I start Firebase Admin for pushes and storage deletes, or leave it off if credentials are missing.
What the function is responsible for: I initialize FirebaseApp once at startup.
Where it works: I run after Spring builds the bean. PushNotificationService and FirebaseStorageService later check FirebaseApp.getApps().
Why I chose to write the code of the function the way its written: I skip if an app already exists. I log a warning instead of crashing so the API still starts without Firebase.
More: I use GoogleCredentials, FirebaseOptions, FirebaseApp, and slf4j LoggerFactory.

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

What that function does: I load credentials from FIREBASE_SERVICE_ACCOUNT or from application default credentials.
What the function is responsible for: I am the credential source for initialize.
Where it works: I am called by initialize. I am unused except internally.
Why I chose to write the code of the function the way its written: I wrote it this way because a file path is easy for local runs. Default credentials cover hosted environments.
More: I use System.getenv, FileInputStream, GoogleCredentials.fromStream, and getApplicationDefault.
