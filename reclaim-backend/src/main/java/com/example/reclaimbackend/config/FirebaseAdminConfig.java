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
 * Starts Firebase Admin for pushes and storage deletes.
 * Looks at {@code FIREBASE_SERVICE_ACCOUNT} or default credentials.
 * If neither is set, the app still starts and pushes are skipped.
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
