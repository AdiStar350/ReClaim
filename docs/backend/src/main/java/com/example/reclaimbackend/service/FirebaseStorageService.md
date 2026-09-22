# FirebaseStorageService.java

## Overview
This is a **service** that deletes ReClaim images from **Firebase Storage** when a URL is replaced or removed (see `docs/GLOSSARY.md`). The Android app uploads images itself; only the backend (with Admin credentials) can safely delete old objects. Deletes are limited to `items/` and `profiles/` paths, and skipped if Firebase Admin was never initialized.

## Concepts used here
- **Service** — storage cleanup used by profile/item updates; see `docs/GLOSSARY.md`.
- **Firebase Storage** — cloud file store for photos; see `docs/GLOSSARY.md`.
- **URL parsing** — download URLs are turned into bucket + object path before delete.
- **Graceful skip** — non-Firebase URLs, missing Admin, or already-gone files do not crash updates.

## Libraries and imports
- **Firebase / Google Cloud Storage** (`FirebaseApp`, `StorageClient`, `Bucket`, `Blob`): Admin access to delete objects.
- **Spring** (`@Service`, `HttpStatus`, `ResponseStatusException`): service bean; `502` if delete fails after Admin is up.
- **Java standard** (`URI`, `URLDecoder`, `Pattern`, `Matcher`, collections): parse several URL shapes.
- **SLF4J**: debug/warn/info around skips and deletes.

## Fields
- `log` (`Logger`) — static logger.
- `PRIMARY_BUCKET` / `LEGACY_BUCKET` (`String`) — known ReClaim bucket names tried when deleting.
- `FIREBASE_DOWNLOAD` (`Pattern`) — matches classic `firebasestorage.googleapis.com` download URLs.

## Functions

### `private static String blankToNull(String value)`
```java
private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
        return null;
    }
    return value.trim();
}
```
- **What it does:** returns null for blank strings, else the trimmed value.
- **Where it's called:** `deleteReplacedImage` and `deleteStoredImage` in this class.
- **Why it's written this way:** treats empty URLs as “nothing to delete.”
- **Step by step:**
  1. If null/blank, return null.
  2. Else return trimmed text.

### `private static String stripLeadingSlash(String path)`
```java
private static String stripLeadingSlash(String path) {
    if (path == null) {
        return "";
    }
    return path.startsWith("/") ? path.substring(1) : path;
}
```
- **What it does:** removes a leading `/` from an object path.
- **Where it's called:** `location` and the `storage.googleapis.com` / host-style branches of `parse`.
- **Why it's written this way:** Storage object names should not start with `/`.
- **Step by step:**
  1. Null → empty string.
  2. If starts with `/`, drop it; else return as-is.

### `private static boolean isAppObject(String objectPath)`
```java
private static boolean isAppObject(String objectPath) {
    return objectPath.startsWith("items/") || objectPath.startsWith("profiles/");
}
```
- **What it does:** returns true only for ReClaim item/profile image paths.
- **Where it's called:** `deleteStoredImage`, before attempting delete.
- **Why it's written this way:** refuses to delete arbitrary bucket paths if a weird URL is passed.
- **Step by step:**
  1. Check the path prefix.
  2. Return true only for `items/` or `profiles/`.

### `private static Set<String> candidateBuckets(String parsedBucket)`
```java
private static Set<String> candidateBuckets(String parsedBucket) {
    LinkedHashSet<String> buckets = new LinkedHashSet<>();
    if (parsedBucket != null && !parsedBucket.isBlank()) {
        buckets.add(parsedBucket);
    }
    buckets.add(PRIMARY_BUCKET);
    buckets.add(LEGACY_BUCKET);
    return buckets;
}
```
- **What it does:** builds an ordered set of bucket names to try (parsed first, then primary, then legacy).
- **Where it's called:** `deleteStoredImage`.
- **Why it's written this way:** older uploads may live under the appspot bucket name while new ones use `.firebasestorage.app`.
- **Step by step:**
  1. Add the parsed bucket if present.
  2. Add primary and legacy constants (deduped by `LinkedHashSet`).
  3. Return the set.

### `private static StorageLocation location(String bucket, String objectPath)`
```java
private static StorageLocation location(String bucket, String objectPath) {
    String path = stripLeadingSlash(objectPath);
    if (path.contains("..")) {
        return null;
    }
    return new StorageLocation(bucket, path);
}
```
- **What it does:** builds a `StorageLocation`, rejecting paths that contain `..`.
- **Where it's called:** from `parse` for each successful URL shape.
- **Why it's written this way:** blocks path-traversal style object names.
- **Step by step:**
  1. Strip leading slash.
  2. If path contains `..`, return null.
  3. Otherwise return a new `StorageLocation`.

### `static StorageLocation parse(String url)`
```java
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
```
- **What it does:** turns a download URL into bucket + object path, or null if it is not a recognized Firebase/GCS URL.
- **Where it's called:** `deleteStoredImage` in this class; also `FirebaseStorageServiceTest` (package-visible for tests).
- **Why it's written this way:** the app may store different historical URL formats; one parser covers them.
- **Step by step:**
  1. Reject blank input.
  2. Try the classic Firebase download URL regex (decode the object path).
  3. Else parse as URI for `storage.googleapis.com` or `*.firebasestorage.app` / `*.appspot.com` hosts.
  4. Return null if nothing matches.

### `private boolean deleteFromBucket(String bucketName, String objectPath)`
```java
private boolean deleteFromBucket(String bucketName, String objectPath) {
    Bucket bucket = StorageClient.getInstance().bucket(bucketName);
    Blob blob = bucket.get(objectPath);
    if (blob == null) {
        return false;
    }
    return blob.delete();
}
```
- **What it does:** deletes one object from one bucket; returns false if the blob is already missing.
- **Where it's called:** `deleteStoredImage` for each candidate bucket.
- **Why it's written this way:** missing blob is success-of-a-sort (already gone), not an error.
- **Step by step:**
  1. Get the bucket via `StorageClient`.
  2. Look up the blob; if null, return false.
  3. Call `blob.delete()` and return its result.

### `public void deleteReplacedImage(String previousUrl, String newUrl)`
```java
public void deleteReplacedImage(String previousUrl, String newUrl) {
    String previous = blankToNull(previousUrl);
    String next = blankToNull(newUrl);
    if (previous == null || previous.equals(next)) {
        return;
    }
    deleteStoredImage(previous);
}
```
- **What it does:** deletes the previous image only when it is non-blank and different from the new URL.
- **Where it's called:** `UserService.updateProfile` and `ItemService.updateItem`.
- **Why it's written this way:** avoids deleting when the URL did not change or there was no previous file.
- **Step by step:**
  1. Normalize both URLs with `blankToNull`.
  2. Return if previous is null or equal to next.
  3. Call `deleteStoredImage(previous)`.

### `public void deleteStoredImage(String downloadUrl)`
```java
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
```
- **What it does:** parses the URL, safety-checks the path, and deletes across candidate buckets; throws `502` only if every attempt errored.
- **Where it's called:** `deleteReplacedImage` in this class (public so tests/other callers can delete by URL directly).
- **Why it's written this way:** tries multiple bucket names; treats “already absent” as OK; fails the API update only when Storage is reachable but delete keeps throwing.
- **Step by step:**
  1. Normalize URL; return if blank.
  2. Parse location; skip non-Firebase URLs.
  3. Refuse paths outside `items/` / `profiles/`.
  4. Skip if Firebase Admin is not initialized.
  5. Try each candidate bucket via `deleteFromBucket`.
  6. If a delete succeeds, return; if only errors occurred, throw 502; else log “already absent.”
