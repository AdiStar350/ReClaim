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

What that function does: I delete the old image URL when it is being replaced by a different one.
What the function is responsible for: I skip blank, same, or missing previous URLs, then delete the old file.
Where it works: I am called by UserService.updateProfile and ItemService.updateItem.
Why I chose to write the code of the function the way its written: I compare trimmed URLs so a no-op update does not hit storage.
More: I use blankToNull and deleteStoredImage.

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

What that function does: I delete a ReClaim image from Firebase Storage if the URL is ours and the path is under items or profiles.
What the function is responsible for: I parse the URL, try known buckets, and throw only if delete fails for a reason other than the file already being gone.
Where it works: I am called by deleteReplacedImage. I am public but I found no other Java callers.
Why I chose to write the code of the function the way its written: I refuse paths outside items/ and profiles/ so a bad URL cannot delete other objects. I try more than one bucket name because old links used appspot.com.
More: I use parse, isAppObject, candidateBuckets, FirebaseApp.getApps, and HttpStatus.BAD_GATEWAY.

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

What that function does: I delete one object from a named bucket, or return false if it is already gone.
What the function is responsible for: I talk to Firebase Storage for one bucket.
Where it works: I am called by deleteStoredImage. I am unused except internally.
Why I chose to write the code of the function the way its written: I wrote it this way because false for a missing blob lets the caller try the next bucket without treating "already gone" as a failure.
More: I use StorageClient.getInstance, Bucket.get, and Blob.delete.

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

What that function does: I turn a download URL into a bucket and object path, or null if it is not a Firebase URL.
What the function is responsible for: I understand the URL shapes our app stores.
Where it works: I am called by deleteStoredImage. FirebaseStorageServiceTest checks me with several URLs.
Why I chose to write the code of the function the way its written: I handle the encoded firebasestorage.googleapis.com form and the simpler host/path forms so old and new links both parse.
More: I use Pattern FIREBASE_DOWNLOAD, URLDecoder, URI.create, and Locale.ROOT.

```java
private static StorageLocation location(String bucket, String objectPath) {
    String path = stripLeadingSlash(objectPath);
    if (path.contains("..")) {
        return null;
    }
    return new StorageLocation(bucket, path);
}
```

What that function does: I build a StorageLocation after stripping a leading slash and rejecting .. in the path.
What the function is responsible for: I stop path tricks before a delete.
Where it works: I am called by parse. I am unused except internally.
Why I chose to write the code of the function the way its written: I wrote it this way because rejecting .. is a simple guard so a crafted URL cannot walk out of the object path.
More: I use stripLeadingSlash and the StorageLocation record.

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

What that function does: I list buckets to try: the one in the URL, then the current bucket, then the old appspot name.
What the function is responsible for: I keep delete working when the URL bucket and the live bucket differ.
Where it works: I am called by deleteStoredImage. I am unused except internally.
Why I chose to write the code of the function the way its written: I wrote it this way because linkedHashSet keeps order and drops duplicates if the URL already used the primary name.
More: I use LinkedHashSet, PRIMARY_BUCKET, and LEGACY_BUCKET.

```java
private static boolean isAppObject(String objectPath) {
    return objectPath.startsWith("items/") || objectPath.startsWith("profiles/");
}
```

What that function does: I return true only for objects under items/ or profiles/.
What the function is responsible for: I limit deletes to ReClaim image folders.
Where it works: I am called by deleteStoredImage. I am unused except internally.
Why I chose to write the code of the function the way its written: I wrote it this way because prefix checks are enough because those are the folders the app writes to.
More: I use String.startsWith.

```java
private static String stripLeadingSlash(String path) {
    if (path == null) {
        return "";
    }
    return path.startsWith("/") ? path.substring(1) : path;
}
```

What that function does: I remove a leading slash from a path, or return empty if the path is null.
What the function is responsible for: I normalize object paths from URLs.
Where it works: I am called by parse and location. I am unused except internally.
Why I chose to write the code of the function the way its written: I wrote it this way because storage object names do not start with /.
More: I use substring(1) when needed.

```java
private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
        return null;
    }
    return value.trim();
}
```

What that function does: I turn null or blank into null, otherwise I trim.
What the function is responsible for: I treat empty URLs as missing.
Where it works: I am called by deleteReplacedImage and deleteStoredImage. I am unused except internally.
Why I chose to write the code of the function the way its written: I wrote it this way because same helper as UserService so blank picture fields mean "no URL".
More: I use String.isBlank and trim.

```java
record StorageLocation(String bucket, String objectPath) {
}
```

What that function does: I hold a bucket name and object path together.
What the function is responsible for: I am the parse result. Java gives me bucket() and objectPath().
Where it works: I am returned by parse. deleteStoredImage and FirebaseStorageServiceTest read my fields.
Why I chose to write the code of the function the way its written: I wrote it this way because a nested record is enough; I did not need extra methods.
More: I am a Java record. Tests call location.bucket() and location.objectPath().
