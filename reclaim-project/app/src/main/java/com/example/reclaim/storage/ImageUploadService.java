package com.example.reclaim.storage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Uploads compressed JPEG item images to Firebase Storage.
 * <p>
 * Images are stored at {@code items/{userId}/{uuid}.jpg}. The client tries
 * both {@code reclaim-5b4fd.firebasestorage.app} and the legacy
 * {@code reclaim-5b4fd.appspot.com} bucket names, then returns a download URL
 * for MongoDB.
 * </p>
 */
public final class ImageUploadService {

    private static final String TAG = "ImageUpload";
    private static final String PROJECT_ID = "reclaim-5b4fd";
    private static final int MAX_IMAGE_DIMENSION = 1280;
    private static final int JPEG_QUALITY = 80;
    private static final int UPLOAD_TIMEOUT_SECONDS = 60;

    private ImageUploadService() {
    }

    /**
     * Compresses and uploads an image, returning its Firebase download URL.
     *
     * @param context application context
     * @param imageUri local content URI of the image
     * @param userId   authenticated user ID used in the storage path
     * @return download URL on success
     * @throws IOException if the image cannot be read or the network upload fails
     */
    @NonNull
    public static String uploadImage(@NonNull Context context,
                                     @NonNull Uri imageUri,
                                     @NonNull String userId) throws IOException {
        Log.d(TAG, "Starting upload uri=" + imageUri + " userId=" + userId);

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
                String url = uploadToBucket(app, gsUri, compressed, safeUserId, fileName);
                Log.d(TAG, "Upload complete bucket=" + gsUri + " url=" + url);
                return url;
            } catch (DownloadUrlFallbackException e) {
                fallbackMediaUrl = e.mediaUrl;
                lastError = e;
                Log.w(TAG, "getDownloadUrl 404 on " + gsUri
                        + " after putBytes; trying Storage domain alias. fallback=" + e.mediaUrl);
            } catch (Exception e) {
                lastError = e;
                logStorageFailure("uploadToBucket " + gsUri, "items/" + safeUserId + "/" + fileName, e);
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

    @NonNull
    private static String uploadToBucket(@NonNull FirebaseApp app,
                                         @NonNull String gsUri,
                                         @NonNull byte[] compressed,
                                         @NonNull String userId,
                                         @NonNull String fileName) throws Exception {
        FirebaseStorage storage = FirebaseStorage.getInstance(app, gsUri);
        StorageReference reference = storage.getReference()
                .child("items")
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

    private static final class DownloadUrlFallbackException extends Exception {
        final String mediaUrl;

        DownloadUrlFallbackException(@NonNull String mediaUrl, @Nullable Throwable cause) {
            super(cause);
            this.mediaUrl = mediaUrl;
        }
    }

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

    @NonNull
    private static String gs(@NonNull String bucket) {
        return bucket.startsWith("gs://") ? bucket : "gs://" + bucket;
    }

    /**
     * Firebase Storage default rules require {@code request.auth != null}.
     * ReClaim authenticates with a Spring JWT, so an anonymous Firebase
     * session is created only for Storage. If Anonymous Auth is disabled
     * in the console, the upload still proceeds and depends on storage
     * rules that allow JPEG writes under {@code items/}.
     */
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

    @Nullable
    private static Throwable unwrap(@Nullable Throwable error) {
        Throwable current = error;
        while (current instanceof java.util.concurrent.ExecutionException
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

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
}
