package com.example.reclaimbackend.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.StorageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deletes ReClaim images from storage. The app cannot delete them itself,
 * so this runs before the new URL is saved.
 */
@Service
public class FirebaseStorageService {

    private static final Logger log = LoggerFactory.getLogger(FirebaseStorageService.class);

    static final String PRIMARY_BUCKET = "reclaim-5b4fd.firebasestorage.app";
    static final String LEGACY_BUCKET = "reclaim-5b4fd.appspot.com";

    private static final Pattern FIREBASE_DOWNLOAD = Pattern.compile(
            "^https://firebasestorage\\.googleapis\\.com/v0/b/([^/]+)/o/([^?]+)",
            Pattern.CASE_INSENSITIVE);

    /**
     * Deletes {@code previousUrl} when it is being replaced.
     * Skips blank, same, or non-Firebase links. Throws if delete fails
     * for a reason other than the file already being gone.
     */
    public void deleteReplacedImage(String previousUrl, String newUrl) {
        String previous = blankToNull(previousUrl);
        String next = blankToNull(newUrl);
        if (previous == null || previous.equals(next)) {
            return;
        }
        deleteStoredImage(previous);
    }

    public void deleteStoredImage(String downloadUrl) {
        String url = blankToNull(downloadUrl);
        if (url == null) {
            return;
        }
        StorageLocation location = parse(url);
        if (location == null) {
            log.debug("Skipping storage delete for non-Firebase URL");
            return;
        }
        if (!isAppObject(location.objectPath)) {
            log.warn("Refusing to delete storage path outside items/profiles: {}",
                    location.objectPath);
            return;
        }
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase Admin is not initialized; cannot delete {}",
                    location.objectPath);
            return;
        }

        Exception lastError = null;
        for (String bucketName : candidateBuckets(location.bucket)) {
            try {
                if (deleteFromBucket(bucketName, location.objectPath)) {
                    log.info("Deleted replaced Storage object gs://{}/{}",
                            bucketName, location.objectPath);
                    return;
                }
            } catch (Exception e) {
                lastError = e;
                log.warn("Storage delete failed gs://{}/{}: {}",
                        bucketName, location.objectPath, e.getMessage());
            }
        }

        if (lastError != null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Could not delete the previous image from storage", lastError);
        }
        log.info("Storage object already absent for {}", location.objectPath);
    }

    private boolean deleteFromBucket(String bucketName, String objectPath) {
        Bucket bucket = StorageClient.getInstance().bucket(bucketName);
        Blob blob = bucket.get(objectPath);
        if (blob == null) {
            return false;
        }
        return blob.delete();
    }

    static StorageLocation parse(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        Matcher firebase = FIREBASE_DOWNLOAD.matcher(url.trim());
        if (firebase.find()) {
            String path = URLDecoder.decode(firebase.group(2), StandardCharsets.UTF_8);
            return location(firebase.group(1), path);
        }
        try {
            URI uri = URI.create(url.trim());
            String host = uri.getHost();
            String path = uri.getPath();
            if (host == null || path == null) {
                return null;
            }
            if ("storage.googleapis.com".equalsIgnoreCase(host)) {
                String stripped = stripLeadingSlash(path);
                int slash = stripped.indexOf('/');
                if (slash <= 0) {
                    return null;
                }
                return location(stripped.substring(0, slash), stripped.substring(slash + 1));
            }
            if (host.toLowerCase(Locale.ROOT).endsWith(".firebasestorage.app")
                    || host.toLowerCase(Locale.ROOT).endsWith(".appspot.com")) {
                return location(host, stripLeadingSlash(path));
            }
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        return null;
    }

    private static StorageLocation location(String bucket, String objectPath) {
        String path = stripLeadingSlash(objectPath);
        if (path.contains("..")) {
            return null;
        }
        return new StorageLocation(bucket, path);
    }

    private static Set<String> candidateBuckets(String parsedBucket) {
        LinkedHashSet<String> buckets = new LinkedHashSet<>();
        if (parsedBucket != null && !parsedBucket.isBlank()) {
            buckets.add(parsedBucket);
        }
        buckets.add(PRIMARY_BUCKET);
        buckets.add(LEGACY_BUCKET);
        return buckets;
    }

    private static boolean isAppObject(String objectPath) {
        return objectPath.startsWith("items/") || objectPath.startsWith("profiles/");
    }

    private static String stripLeadingSlash(String path) {
        if (path == null) {
            return "";
        }
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    record StorageLocation(String bucket, String objectPath) {
    }
}
