```java
private ImageUploadService() {
}
```

What that function does: I stop anyone from making an instance of this helper.
What the function is responsible for: I keep uploads as static methods.
Where it works: I am not called. Java uses me when someone tries `new ImageUploadService()`.
Why I chose to write the code of the function the way its written: I made the constructor private so callers use uploadImage.
More: I use a private constructor. The class is final.

```java
@NonNull
public static String uploadImage(@NonNull Context context,
                                 @NonNull Uri imageUri,
                                 @NonNull String userId) throws IOException {
    return uploadImage(context, imageUri, userId, FOLDER_ITEMS);
}
```

What that function does: I shrink and send an item photo, then return the download URL.
What the function is responsible for: I am the default upload into the items folder.
Where it works: I am called by ReportActivity and ReportSyncWorker.
Why I chose to write the code of the function the way its written: I forward to the four-arg method with FOLDER_ITEMS so item photos share one path.
More: I reuse uploadImage with FOLDER_ITEMS.

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

What that function does: I compress the photo, try known Storage buckets, and return a URL.
What the function is responsible for: I am the full upload path for item and profile photos.
Where it works: I am called by the three-arg uploadImage, RegisterActivity, and EditProfileActivity with FOLDER_PROFILES.
Why I chose to write the code of the function the way its written: I try both bucket names because the project can use .appspot.com or .firebasestorage.app. A 404 on getDownloadUrl can still use the media URL.
More: I use compressImage, firebaseApp, ensureFirebaseAuth, candidateBuckets, and UUID file names.

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

What that function does: I put JPEG bytes into one bucket and then ask for a download URL.
What the function is responsible for: I am one try of the bucket loop.
Where it works: I am called by uploadImage for each candidate bucket.
Why I chose to write the code of the function the way its written: I wait with Tasks.await and a timeout so the caller stays a blocking method. A 404 after putBytes becomes DownloadUrlFallbackException.
More: I use FirebaseStorage, StorageReference.child, putBytes, getDownloadUrl, and StorageMetadata content type image/jpeg.

```java
DownloadUrlFallbackException(@NonNull String mediaUrl, @Nullable Throwable cause) {
    super(cause);
    this.mediaUrl = mediaUrl;
}
```

What that function does: I keep a media URL when getDownloadUrl fails after a successful put.
What the function is responsible for: I am a private signal, not a user-facing error.
Where it works: I am thrown from uploadToBucket and caught in uploadImage.
Why I chose to write the code of the function the way its written: A nested exception lets me pass the fallback URL without a Pair.
More: I extend Exception and store mediaUrl.

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

What that function does: I return the default FirebaseApp, starting it from context if needed.
What the function is responsible for: I make sure Storage has an app instance.
Where it works: I am called by uploadImage.
Why I chose to write the code of the function the way its written: A worker thread may run before Application init, so I catch IllegalStateException.
More: I use FirebaseApp.getInstance and initializeApp.

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

What that function does: I list bucket URIs to try, configured first, then both project domain forms.
What the function is responsible for: I give uploadImage a stable try order without duplicates.
Where it works: I am called by uploadImage.
Why I chose to write the code of the function the way its written: LinkedHashSet keeps order and drops repeats. Both domains exist on some Firebase projects.
More: I use FirebaseOptions.getStorageBucket and PROJECT_ID reclaim-5b4fd.

```java
@NonNull
private static String gs(@NonNull String bucket) {
    return bucket.startsWith("gs://") ? bucket : "gs://" + bucket;
}
```

What that function does: I add the gs:// prefix if it is missing.
What the function is responsible for: I make a bucket name valid for FirebaseStorage.getInstance.
Where it works: I am called by candidateBuckets.
Why I chose to write the code of the function the way its written: google-services.json may store the bucket without gs://.
More: I return the same string when the prefix is already there.

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

What that function does: I sign into Firebase anonymously so Storage can accept the write.
What the function is responsible for: I get an id token when anonymous auth is on.
Where it works: I am called by uploadImage before putBytes.
Why I chose to write the code of the function the way its written: I log and continue if anonymous sign-in is off, so storage rules can still allow the write.
More: I use FirebaseAuth.signInAnonymously, getIdToken, and Tasks.await.

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

What that function does: I build a Storage media URL from the object path and optional token.
What the function is responsible for: I am the fallback URL when getDownloadUrl 404s.
Where it works: I am called by uploadToBucket when throwing DownloadUrlFallbackException.
Why I chose to write the code of the function the way its written: alt=media is the public object URL form. Uri.encode keeps the path safe.
More: I use StorageReference.getBucket, getPath, and downloadToken.

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

What that function does: I read the first download token from Storage metadata.
What the function is responsible for: I attach a token to the media URL when Firebase stored one.
Where it works: I am called by mediaUrl.
Why I chose to write the code of the function the way its written: I try both metadata keys Firebase has used. I take the first token if there are several.
More: I use StorageMetadata.getCustomMetadata.

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

What that function does: I return true if the error is a missing object or bucket, or HTTP 404.
What the function is responsible for: I decide if the next bucket alias should be tried.
Where it works: I am called by uploadImage and uploadToBucket.
Why I chose to write the code of the function the way its written: Other errors should stop the loop. 404 can mean the wrong domain.
More: I use StorageException error codes and getHttpResultCode.

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

What that function does: I log a Storage failure with path, code, and HTTP status when I can.
What the function is responsible for: I make upload bugs easier to read in logcat.
Where it works: I am called by uploadImage and uploadToBucket.
Why I chose to write the code of the function the way its written: I unwrap to StorageException so the Firebase code is visible.
More: I use Log.e and findStorageException.

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

What that function does: I walk the cause chain until I find a StorageException.
What the function is responsible for: I pull the Firebase error out of Tasks.await wrappers.
Where it works: I am called by isMissingObjectOrBucket, logStorageFailure, and describeFailure.
Why I chose to write the code of the function the way its written: Tasks wraps errors, so I look at causes.
More: I use unwrap first, then getCause.

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

What that function does: I peel ExecutionException layers from Tasks.await.
What the function is responsible for: I give findStorageException the real error.
Where it works: I am called by findStorageException.
Why I chose to write the code of the function the way its written: Tasks.await wraps failures in ExecutionException.
More: I loop while the type is ExecutionException.

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

What that function does: I turn a Storage error into a short message the UI can show.
What the function is responsible for: I explain 403 and 404 in plain words.
Where it works: I am called by uploadImage when throwing IOException.
Why I chose to write the code of the function the way its written: Permission and missing-bucket errors are the ones I have seen most, so I spell those out.
More: I use StorageException codes and HTTP result codes.

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

What that function does: I decode, rotate, shrink, and JPEG-compress the photo.
What the function is responsible for: I keep uploads small and upright.
Where it works: I am called by uploadImage.
Why I chose to write the code of the function the way its written: 1280px and quality 80 are enough for a report photo. I recycle bitmaps to save memory.
More: I use decodeSampledBitmap, applyExifOrientation, Bitmap.createScaledBitmap, and CompressFormat.JPEG.

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

What that function does: I rotate the bitmap to match the photo's EXIF orientation.
What the function is responsible for: I stop sideways phone photos from uploading on their side.
Where it works: I am called by compressImage.
Why I chose to write the code of the function the way its written: I only handle 90/180/270. Other tags keep the bitmap as decoded.
More: I use android.media.ExifInterface, Matrix.postRotate, and Bitmap.createBitmap.

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

What that function does: I decode the photo at a smaller sample size so a large file does not fill memory.
What the function is responsible for: I am the first decode step before rotate and JPEG compress.
Where it works: I am called by compressImage.
Why I chose to write the code of the function the way its written: I read bounds first with inJustDecodeBounds, then decode with inSampleSize. I open the stream twice because decodeStream consumes it.
More: I use BitmapFactory.Options, ContentResolver.openInputStream, and inSampleSize powers of two.
