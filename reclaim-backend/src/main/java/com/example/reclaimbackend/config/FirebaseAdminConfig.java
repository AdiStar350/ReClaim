package com.example.reclaimbackend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Initializes the Firebase Admin SDK used for sending FCM push notifications.
 * <p>
 * Credentials are resolved from the {@code FIREBASE_SERVICE_ACCOUNT} env var
 * (path to a service-account JSON) or Application Default Credentials
 * ({@code GOOGLE_APPLICATION_CREDENTIALS}). When neither is available the
 * backend still starts; pushes are simply skipped.
 * </p>
 */
@Configuration
public class FirebaseAdminConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseAdminConfig.class);

    @PostConstruct
    public void initialize() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }
        try {
            GoogleCredentials credentials = loadCredentials();
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("Firebase Admin initialized; push notifications enabled");
        } catch (IOException e) {
            log.warn("Firebase Admin not initialized ({}); push notifications disabled. "
                    + "Set FIREBASE_SERVICE_ACCOUNT to a service-account JSON path to enable.",
                    e.getMessage());
        }
    }

    private GoogleCredentials loadCredentials() throws IOException {
        String path = System.getenv("FIREBASE_SERVICE_ACCOUNT");
        if (path != null && !path.isBlank()) {
            try (FileInputStream stream = new FileInputStream(path)) {
                return GoogleCredentials.fromStream(stream);
            }
        }
        return GoogleCredentials.getApplicationDefault();
    }
}
