# ImageUploadService.java

## Overview
This file is a **static helper** that shrinks a local photo to JPEG and uploads it to
**Firebase Storage**, then returns a download URL. Paths look like
`items/{userId}/{uuid}.jpg` or `profiles/{userId}/{uuid}.jpg`. It tries both common
bucket domain aliases, signs into Firebase anonymously when possible, and falls back
to a constructed media URL if `getDownloadUrl` 404s after a successful put.

## Concepts used here
- **Firebase Storage.** Cloud file store for photos; see Firebase in
  `docs/GLOSSARY.md`.
- **`Context` / `Uri`.** Opens the picked image stream; see `docs/GLOSSARY.md`.
- **Bitmap / EXIF.** Decode, fix camera orientation, scale, then JPEG-compress before
  upload so large phone photos do not stall the network.
- **Anonymous Firebase Auth.** Optional sign-in so Storage rules that require an auth
  user still accept the write.
- **Static utility class.** Private constructor; public folder constants and upload
  overloads.

## Libraries and imports
- **Android (`Context`, `Bitmap`, `BitmapFactory`, `Matrix`, `ExifInterface`, `Uri`,
  `Log`).** Read, rotate, scale, and compress images.
- **AndroidX annotations (`NonNull`, `Nullable`).** Null contracts.
- **Firebase (`FirebaseApp`, `FirebaseAuth`, `FirebaseStorage`, `StorageReference`,
  `StorageMetadata`, `UploadTask`, `StorageException`, `Tasks`).** Auth, upload, and
  download URL.
- **Java (`ByteArrayOutputStream`, `IOException`, `InputStream`, `LinkedHashSet`,
  `UUID`, `TimeUnit`).** Compression buffer, stream I/O, bucket candidate set, unique
  file names, timeouts.

## Fields
- `private static final String TAG = "ImageUpload"` — log tag.
- `private static final String PROJECT_ID = "reclaim-5b4fd"` — Firebase project id used
  when building fallback bucket URIs.
- `private static final int MAX_IMAGE_DIMENSION = 1280` — longest side after scaling /
  sampling.
- `private static final int JPEG_QUALITY = 80` — JPEG compression quality.
- `private static final int UPLOAD_TIMEOUT_SECONDS = 60` — await timeout for put and
  download-URL tasks.
- `public static final String FOLDER_ITEMS = "items"` — Storage folder for report
  photos.
- `public static final String FOLDER_PROFILES = "profiles"` — Storage folder for
  profile photos.

## Functions

### `private ImageUploadService()`
```java
private ImageUploadService() {
}
```
- **What it does:** empty private constructor blocking `new ImageUploadService()`.
- **Where it's called:** nothing calls it; it only stops instances from being made.
- **Why it's written this way:** static-only helper.
- **Step by step:** 1) private access; 2) empty body; 3) no instances from outside.

### `private static final class DownloadUrlFallbackException`
```java
private static final class DownloadUrlFallbackException extends Exception {
    final String mediaUrl;

    DownloadUrlFallbackException(@NonNull String mediaUrl, @Nullable Throwable cause) {
        super(cause);
        this.mediaUrl = mediaUrl;
    }
}
```
- **What it does:** special exception carrying a constructed media URL when
  `getDownloadUrl` fails with a "missing" error after a successful upload.
- **Where it's called:** thrown from `uploadToBucket`; caught in the public
  `uploadImage` loop.
- **Why it's written this way:** separates "upload worked but token URL failed" from
  real upload failures so the outer loop can keep the media URL as a last resort.
- **Step by step:** 1) store `mediaUrl`; 2) pass `cause` to `Exception`.

### `private static FirebaseApp firebaseApp(@NonNull Context context)`
```java
@NonNull
private static FirebaseApp firebaseApp(@NonNull Context context) {
    try {
        return FirebaseApp.getInstance();
    } catch (IllegalStateException e) {
        Log.w(TAG, "FirebaseApp not initialized; initializing from context", e);
        FirebaseApp.initializeApp(context.getApplicationContext());
        return FirebaseApp.getInstance();
    }
}
```
- **What it does:** returns the default FirebaseApp, initializing it from the app
  context if needed.
- **Where it's called:** from the public `uploadImage` overload before auth and
  bucket upload.
- **Why it's written this way:** some call paths may run before Firebase was warmed;
  lazy init avoids a hard crash.
- **Step by step:** 1) try `getInstance`; 2) on `IllegalStateException`,
  `initializeApp` and get again.

### `private static String gs(@NonNull String bucket)`
```java
@NonNull
private static String gs(@NonNull String bucket) {
    return bucket.startsWith("gs://") ? bucket : "gs://" + bucket;
}
```
- **What it does:** normalizes a bucket name to a `gs://` URI.
- **Where it's called:** inside `candidateBuckets` when adding configured and
  alternate domains.
- **Why it's written this way:** Firebase Storage APIs expect the `gs://` form.
- **Step by step:** 1) if already prefixed, return as-is; 2) else prepend `gs://`.

### `private static String[] candidateBuckets(@NonNull FirebaseApp app)`
```java
@NonNull
private static String[] candidateBuckets(@NonNull FirebaseApp app) {
    LinkedHashSet<String> buckets = new LinkedHashSet<>();
    String configured = app.getOptions().getStorageBucket();
    if (configured != null && !configured.isEmpty()) {
        buckets.add(gs(configured));
        if (configured.contains(".firebasestorage.app")) {
            buckets.add(gs(configured.replace(".firebasestorage.app", ".appspot.com")));
        } else if (configured.contains(".appspot.com")) {
            buckets.add(gs(configured.replace(".appspot.com", ".firebasestorage.app")));
        }
    }
    buckets.add("gs://" + PROJECT_ID + ".firebasestorage.app");
    buckets.add("gs://" + PROJECT_ID + ".appspot.com");
    return buckets.toArray(new String[0]);
}
```
- **What it does:** builds an ordered, de-duplicated list of bucket URIs to try
  (configured bucket, domain alias, then project defaults).
- **Where it's called:** public `uploadImage` when looping upload attempts.
- **Why it's written this way:** Google has used both `.appspot.com` and
  `.firebasestorage.app`; trying both avoids 404s from picking the wrong alias.
- **Step by step:**
  1. Start a `LinkedHashSet` for order + uniqueness.
  2. Add the configured bucket and its domain twin if present.
  3. Add both project-id default buckets.
  4. Return as a string array.

### `private static void ensureFirebaseAuth(@NonNull FirebaseApp app)`
```java
private static void ensureFirebaseAuth(@NonNull FirebaseApp app) {
    FirebaseAuth auth = FirebaseAuth.getInstance(app);
    try {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Tasks.await(auth.signInAnonymously(), 20, TimeUnit.SECONDS);
            user = auth.getCurrentUser();
        }
        if (user != null) {
            Tasks.await(user.getIdToken(true), 20, TimeUnit.SECONDS);
            Log.d(TAG, "Firebase Auth ready uid=" + user.getUid()
                    + " anonymous=" + user.isAnonymous());
        }
    } catch (Exception e) {
        Log.w(TAG, "Anonymous Firebase Auth failed. Enable Anonymous sign-in "
                + "in Firebase Console, or deploy storage.rules that allow "
                + "unauthenticated JPEG writes under items/{userId}/{fileName}", e);
    }
}
```
- **What it does:** signs in anonymously (if needed) and refreshes an ID token so
  Storage can accept the write under auth-based rules.
- **Where it's called:** public `uploadImage` before uploading bytes.
- **Why it's written this way:** failures only log — upload still proceeds and relies
  on open storage rules if anonymous auth is off in the console.
- **Step by step:**
  1. Get `FirebaseAuth` for the app.
  2. If no current user, await anonymous sign-in.
  3. If a user exists, force-refresh the ID token and log.
  4. On any exception, warn with setup hints.

### `private static String downloadToken(@Nullable StorageMetadata metadata)`
```java
@Nullable
private static String downloadToken(@Nullable StorageMetadata metadata) {
    if (metadata == null) {
        return null;
    }
    String tokens = metadata.getCustomMetadata("downloadTokens");
    if (tokens == null || tokens.isEmpty()) {
        tokens = metadata.getCustomMetadata("firebaseStorageDownloadTokens");
    }
    if (tokens == null || tokens.isEmpty()) {
        return null;
    }
    int comma = tokens.indexOf(',');
    return comma >= 0 ? tokens.substring(0, comma).trim() : tokens.trim();
}
```
- **What it does:** reads the first download token from upload metadata, if any.
- **Where it's called:** by `mediaUrl` when building a fallback HTTPS link.
- **Why it's written this way:** token key names vary; taking the first comma-separated
  value matches Firebase's usual format.
- **Step by step:** 1) null metadata → null; 2) try both metadata keys; 3) return the
  first token before a comma, or the whole string.

### `private static String mediaUrl(...)`
```java
@NonNull
private static String mediaUrl(@NonNull StorageReference reference,
                              @Nullable StorageMetadata metadata) {
    String path = reference.getPath();
    if (path.startsWith("/")) {
        path = path.substring(1);
    }
    String url = "https://firebasestorage.googleapis.com/v0/b/"
            + reference.getBucket()
            + "/o/"
            + Uri.encode(path)
            + "?alt=media";
    String token = downloadToken(metadata);
    if (token != null) {
        url += "&token=" + token;
    }
    return url;
}
```
- **What it does:** builds a direct media download URL for the uploaded object,
  optionally with a token query param.
- **Where it's called:** when wrapping a `DownloadUrlFallbackException` in
  `uploadToBucket`.
- **Why it's written this way:** if the SDK's `getDownloadUrl` 404s, this still gives
  callers a usable link after a successful `putBytes`.
- **Step by step:** 1) strip leading `/` from path; 2) build the REST media URL; 3)
  append token if present; 4) return.

### `private static Throwable unwrap(@Nullable Throwable error)`
```java
@Nullable
private static Throwable unwrap(@Nullable Throwable error) {
    Throwable current = error;
    while (current instanceof java.util.concurrent.ExecutionException
            && current.getCause() != null) {
        current = current.getCause();
    }
    return current;
}
```
- **What it does:** peels `ExecutionException` wrappers from `Tasks.await` failures.
- **Where it's called:** by `findStorageException`.
- **Why it's written this way:** the real `StorageException` is often nested under
  await's wrapper.
- **Step by step:** 1) while current is `ExecutionException` with a cause, walk to
  the cause; 2) return the inner throwable.

### `private static StorageException findStorageException(@Nullable Throwable error)`
```java
@Nullable
private static StorageException findStorageException(@Nullable Throwable error) {
    Throwable current = unwrap(error);
    while (current != null) {
        if (current instanceof StorageException) {
            return (StorageException) current;
        }
        current = current.getCause();
    }
    return null;
}
```
- **What it does:** walks the cause chain for a `StorageException`.
- **Where it's called:** `isMissingObjectOrBucket`, `logStorageFailure`, and
  `describeFailure`.
- **Why it's written this way:** one helper for detecting Storage-specific error
  codes/HTTP status.
- **Step by step:** 1) unwrap; 2) walk causes until a `StorageException` or null.

### `private static boolean isMissingObjectOrBucket(@Nullable Throwable error)`
```java
private static boolean isMissingObjectOrBucket(@Nullable Throwable error) {
    StorageException storageException = findStorageException(error);
    if (storageException == null) {
        return false;
    }
    int code = storageException.getErrorCode();
    return code == StorageException.ERROR_OBJECT_NOT_FOUND
            || code == StorageException.ERROR_BUCKET_NOT_FOUND
            || storageException.getHttpResultCode() == 404;
}
```
- **What it does:** returns true if the failure looks like a missing object/bucket or
  HTTP 404 (worth trying another bucket or media-URL fallback).
- **Where it's called:** public `uploadImage` catch blocks and `uploadToBucket` when
  deciding whether to throw `DownloadUrlFallbackException`.
- **Why it's written this way:** only "not found" style errors should trigger alias
  retries; permission errors should fail fast.
- **Step by step:** 1) find StorageException; 2) check object/bucket-not-found codes
  or HTTP 404.

### `private static void logStorageFailure(...)`
```java
private static void logStorageFailure(@NonNull String stage,
                                      @NonNull String objectPath,
                                      @Nullable Throwable error) {
    if (error == null) {
        Log.e(TAG, stage + " failed with null throwable path=" + objectPath);
        return;
    }
    StorageException storageException = findStorageException(error);
    if (storageException != null) {
        Log.e(TAG, stage + " failed path=" + objectPath
                + " errorCode=" + storageException.getErrorCode()
                + " http=" + storageException.getHttpResultCode()
                + " message=" + storageException.getMessage(), storageException);
    } else {
        Log.e(TAG, stage + " failed path=" + objectPath
                + " type=" + error.getClass().getName()
                + " message=" + error.getMessage(), error);
    }
}
```
- **What it does:** logs a storage failure with stage, path, and Storage error codes
  when available.
- **Where it's called:** public `uploadImage` and `uploadToBucket` on failures.
- **Why it's written this way:** richer logs make bucket/rules misconfig easier to
  debug than a bare message.
- **Step by step:** 1) handle null error; 2) if StorageException, log codes + HTTP; 3)
  else log type and message.

### `private static String describeFailure(@NonNull Throwable error)`
```java
@NonNull
private static String describeFailure(@NonNull Throwable error) {
    StorageException storageException = findStorageException(error);
    if (storageException != null) {
        if (storageException.getErrorCode() == StorageException.ERROR_NOT_AUTHORIZED
                || storageException.getHttpResultCode() == 403) {
            return "Image upload failed: Firebase Storage permission denied (403). "
                    + "Publish storage.rules for reclaim-5b4fd, or enable "
                    + "Anonymous sign-in in Firebase Authentication.";
        }
        if (storageException.getErrorCode() == StorageException.ERROR_OBJECT_NOT_FOUND
                || storageException.getHttpResultCode() == 404) {
            return "Image upload failed: object not found at the Storage location"
                    + " (code=" + storageException.getErrorCode()
                    + ", http=" + storageException.getHttpResultCode()
                    + "). Check that the bucket exists"
                    + " (reclaim-5b4fd.firebasestorage.app or reclaim-5b4fd.appspot.com)"
                    + " and that Storage rules allow read after upload.";
        }
        return "Image upload failed: " + storageException.getMessage()
                + " (code=" + storageException.getErrorCode()
                + ", http=" + storageException.getHttpResultCode() + ")";
    }
    String message = error.getMessage();
    return "Image upload failed: " + (message != null ? message : error.getClass().getSimpleName());
}
```
- **What it does:** turns a throwable into a beginner-friendly `IOException` message
  (especially for 403/404).
- **Where it's called:** public `uploadImage` when giving up after bucket attempts.
- **Why it's written this way:** UI callers can show a hint that points at rules /
  bucket setup instead of an opaque SDK string.
- **Step by step:** 1) prefer StorageException branches for 403 and 404; 2) generic
  Storage message with codes; 3) else use the throwable message or class name.

### `private static Bitmap decodeSampledBitmap(...)`
```java
@Nullable
private static Bitmap decodeSampledBitmap(@NonNull Context context, @NonNull Uri imageUri)
        throws IOException {
    BitmapFactory.Options bounds = new BitmapFactory.Options();
    bounds.inJustDecodeBounds = true;

    try (InputStream boundsStream = context.getContentResolver().openInputStream(imageUri)) {
        if (boundsStream == null) {
            Log.e(TAG, "openInputStream returned null while reading bounds for " + imageUri);
            return null;
        }
        BitmapFactory.decodeStream(boundsStream, null, bounds);
    }

    Log.d(TAG, "Source image mime=" + bounds.outMimeType
            + " " + bounds.outWidth + "x" + bounds.outHeight);

    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        Log.e(TAG, "Decoder could not determine image bounds for " + imageUri);
        return null;
    }

    int height = bounds.outHeight;
    int width = bounds.outWidth;
    int inSampleSize = 1;
    while (height / inSampleSize > MAX_IMAGE_DIMENSION
            || width / inSampleSize > MAX_IMAGE_DIMENSION) {
        inSampleSize *= 2;
    }

    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inSampleSize = inSampleSize;

    try (InputStream imageStream = context.getContentResolver().openInputStream(imageUri)) {
        if (imageStream == null) {
            Log.e(TAG, "openInputStream returned null while decoding " + imageUri);
            return null;
        }
        return BitmapFactory.decodeStream(imageStream, null, options);
    }
}
```
- **What it does:** two-pass decode: read bounds, choose `inSampleSize`, then decode
  a smaller bitmap.
- **Where it's called:** `compressImage`.
- **Why it's written this way:** loading a full camera image into RAM can OOM; sampling
  keeps memory near the 1280px budget.
- **Step by step:**
  1. Decode bounds only.
  2. Bail if stream/bounds invalid.
  3. Grow `inSampleSize` by powers of two until both sides fit under the max.
  4. Decode again with that sample size and return the bitmap.

### `private static Bitmap applyExifOrientation(...)`
```java
@NonNull
private static Bitmap applyExifOrientation(@NonNull Context context,
                                           @NonNull Uri imageUri,
                                           @NonNull Bitmap bitmap) {
    int orientation = ExifInterface.ORIENTATION_NORMAL;
    try (InputStream stream = context.getContentResolver().openInputStream(imageUri)) {
        if (stream != null) {
            ExifInterface exif = new ExifInterface(stream);
            orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        }
    } catch (IOException e) {
        Log.w(TAG, "Could not read EXIF orientation for " + imageUri, e);
        return bitmap;
    }

    Matrix matrix = new Matrix();
    switch (orientation) {
        case ExifInterface.ORIENTATION_ROTATE_90:
            matrix.postRotate(90);
            break;
        case ExifInterface.ORIENTATION_ROTATE_180:
            matrix.postRotate(180);
            break;
        case ExifInterface.ORIENTATION_ROTATE_270:
            matrix.postRotate(270);
            break;
        default:
            return bitmap;
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
}
```
- **What it does:** rotates the bitmap according to EXIF orientation tags from the
  original file.
- **Where it's called:** `compressImage` after decode.
- **Why it's written this way:** many phone JPEGs store upright pixels with a rotate
  tag; without this, uploads look sideways.
- **Step by step:** 1) read EXIF orientation; 2) on IO error return original; 3) for
  90/180/270 build a Matrix and create a rotated bitmap; 4) otherwise return original.

### `private static byte[] compressImage(...)`
```java
@Nullable
private static byte[] compressImage(@NonNull Context context, @NonNull Uri imageUri)
        throws IOException {
    Bitmap bitmap = decodeSampledBitmap(context, imageUri);
    if (bitmap == null) {
        Log.e(TAG, "Could not decode image as a bitmap. uri=" + imageUri
                + " mime=" + context.getContentResolver().getType(imageUri));
        throw new IOException("Unsupported image format. Use JPEG, PNG, or WebP.");
    }

    Bitmap oriented = applyExifOrientation(context, imageUri, bitmap);
    if (oriented != bitmap) {
        bitmap.recycle();
        bitmap = oriented;
    }

    int width = bitmap.getWidth();
    int height = bitmap.getHeight();
    float scale = 1f;
    if (width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION) {
        scale = Math.min(
                (float) MAX_IMAGE_DIMENSION / width,
                (float) MAX_IMAGE_DIMENSION / height);
    }

    Bitmap scaled = bitmap;
    if (scale < 1f) {
        scaled = Bitmap.createScaledBitmap(
                bitmap,
                Math.round(width * scale),
                Math.round(height * scale),
                true);
        if (scaled != bitmap) {
            bitmap.recycle();
        }
    }

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    boolean compressed = scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream);
    scaled.recycle();
    if (!compressed) {
        Log.e(TAG, "Bitmap.compress(JPEG) returned false for uri=" + imageUri);
        return null;
    }
    return outputStream.toByteArray();
}
```
- **What it does:** decode → orient → optional scale → JPEG compress to a byte array.
- **Where it's called:** public `uploadImage` at the start of an upload.
- **Why it's written this way:** smaller JPEGs upload faster and fit Storage rules
  that expect `image/jpeg`; recycling bitmaps frees RAM on low-end phones.
- **Step by step:**
  1. Decode sampled bitmap or throw unsupported format.
  2. Apply EXIF rotation; recycle the old bitmap if replaced.
  3. Scale down if still over max dimension; recycle as needed.
  4. Compress to JPEG quality 80; recycle the scaled bitmap.
  5. Return bytes, or null if compress failed.

### `private static String uploadToBucket(...)`
```java
@NonNull
private static String uploadToBucket(@NonNull FirebaseApp app,
                                     @NonNull String gsUri,
                                     @NonNull byte[] compressed,
                                     @NonNull String userId,
                                     @NonNull String fileName,
                                     @NonNull String folder) throws Exception {
    FirebaseStorage storage = FirebaseStorage.getInstance(app, gsUri);
    StorageReference reference = storage.getReference()
            .child(folder)
            .child(userId)
            .child(fileName);
    String gsPath = reference.toString();
    Log.d(TAG, "Uploading to " + gsPath
            + " bucket=" + reference.getBucket()
            + " path=" + reference.getPath());

    StorageMetadata metadata = new StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build();

    UploadTask.TaskSnapshot snapshot = Tasks.await(
            reference.putBytes(compressed, metadata),
            UPLOAD_TIMEOUT_SECONDS,
            TimeUnit.SECONDS);
    StorageReference uploaded = snapshot.getStorage();
    Log.d(TAG, "putBytes succeeded bytes=" + snapshot.getBytesTransferred()
            + "/" + snapshot.getTotalByteCount()
            + " location=" + uploaded.toString());

    try {
        Uri downloadUri = Tasks.await(
                uploaded.getDownloadUrl(),
                UPLOAD_TIMEOUT_SECONDS,
                TimeUnit.SECONDS);
        if (downloadUri != null) {
            return downloadUri.toString();
        }
        Log.e(TAG, "getDownloadUrl returned null after upload to " + uploaded);
    } catch (Exception downloadError) {
        logStorageFailure("getDownloadUrl", uploaded.toString(), downloadError);
        if (!isMissingObjectOrBucket(downloadError)) {
            throw downloadError;
        }
        throw new DownloadUrlFallbackException(
                mediaUrl(uploaded, snapshot.getMetadata()), downloadError);
    }
    throw new DownloadUrlFallbackException(
            mediaUrl(uploaded, snapshot.getMetadata()),
            new IOException("Firebase returned an empty download URL"));
}
```
- **What it does:** uploads bytes to one bucket path and returns the download URL, or
  throws `DownloadUrlFallbackException` with a media URL if getDownloadUrl fails as
  "missing."
- **Where it's called:** public `uploadImage` for each candidate bucket.
- **Why it's written this way:** `Tasks.await` keeps the call synchronous for
  callers already on a background thread; fallback exception preserves a usable URL
  after a successful put.
- **Step by step:**
  1. Resolve Storage for `gsUri` and child path `folder/userId/fileName`.
  2. `putBytes` with JPEG metadata; await with timeout.
  3. Await `getDownloadUrl`; return string if non-null.
  4. On missing/404 download errors, throw `DownloadUrlFallbackException` with
     `mediaUrl`; rethrow other errors.
  5. If URL was null, also throw the fallback exception.

### `public static String uploadImage(Context, Uri, String)`
```java
@NonNull
public static String uploadImage(@NonNull Context context,
                                 @NonNull Uri imageUri,
                                 @NonNull String userId) throws IOException {
    return uploadImage(context, imageUri, userId, FOLDER_ITEMS);
}
```
- **What it does:** uploads an item (report) photo into the `items` folder and returns
  the download URL.
- **Where it's called:** `ReportActivity` when submitting a report online, and
  `ReportSyncWorker.syncOne` when flushing a queued offline report with a local image.
- **Why it's written this way:** default folder overload so report call sites stay
  short.
- **Step by step:** 1) delegate to the four-arg overload with `FOLDER_ITEMS`.

### `public static String uploadImage(Context, Uri, String, String)`
```java
@NonNull
public static String uploadImage(@NonNull Context context,
                                 @NonNull Uri imageUri,
                                 @NonNull String userId,
                                 @NonNull String folder) throws IOException {
    Log.d(TAG, "Starting upload uri=" + imageUri + " userId=" + userId + " folder=" + folder);

    byte[] compressed = compressImage(context, imageUri);
    if (compressed == null || compressed.length == 0) {
        Log.e(TAG, "Compression produced empty bytes for uri=" + imageUri);
        throw new IOException("Failed to compress image into JPEG");
    }
    Log.d(TAG, "Compressed JPEG size=" + compressed.length + " bytes");

    FirebaseApp app = firebaseApp(context);
    ensureFirebaseAuth(app);

    String safeUserId = userId.replaceAll("[^A-Za-z0-9._-]", "_");
    String fileName = UUID.randomUUID() + ".jpg";
    Exception lastError = null;
    String fallbackMediaUrl = null;

    for (String gsUri : candidateBuckets(app)) {
        try {
            String url = uploadToBucket(app, gsUri, compressed, safeUserId, fileName, folder);
            Log.d(TAG, "Upload complete bucket=" + gsUri + " url=" + url);
            return url;
        } catch (DownloadUrlFallbackException e) {
            fallbackMediaUrl = e.mediaUrl;
            lastError = e;
            Log.w(TAG, "getDownloadUrl 404 on " + gsUri
                    + " after putBytes; trying Storage domain alias. fallback=" + e.mediaUrl);
        } catch (Exception e) {
            lastError = e;
            logStorageFailure("uploadToBucket " + gsUri, folder + "/" + safeUserId + "/" + fileName, e);
            if (!isMissingObjectOrBucket(e)) {
                throw new IOException(describeFailure(e), e);
            }
            Log.w(TAG, "Bucket " + gsUri + " returned 404; trying the Storage domain alias");
        }
    }

    if (fallbackMediaUrl != null) {
        Log.w(TAG, "All getDownloadUrl attempts 404'd; using media URL " + fallbackMediaUrl);
        return fallbackMediaUrl;
    }

    throw new IOException(describeFailure(lastError != null ? lastError
            : new IOException("Image upload failed")), lastError);
}
```
- **What it does:** compresses the image, ensures Firebase auth, then tries each
  candidate bucket until one returns a download URL (or a media-URL fallback).
- **Where it's called:** the three-arg overload (items); also directly from
  `RegisterActivity` and `EditProfileActivity` with `FOLDER_PROFILES`.
- **Why it's written this way:** sanitizing `userId` keeps Storage paths safe; trying
  multiple buckets absorbs domain-alias confusion; keeping `fallbackMediaUrl` means a
  successful put is not wasted if only getDownloadUrl fails.
- **Step by step:**
  1. Compress; throw if empty.
  2. Init FirebaseApp and ensure anonymous auth.
  3. Sanitize user id; pick a random `.jpg` name.
  4. For each candidate bucket: try `uploadToBucket`; on success return URL; on
     download-URL fallback remember media URL and continue; on missing bucket continue;
     on other errors throw described `IOException`.
  5. If a fallback media URL was saved, return it.
  6. Otherwise throw using `describeFailure(lastError)`.
