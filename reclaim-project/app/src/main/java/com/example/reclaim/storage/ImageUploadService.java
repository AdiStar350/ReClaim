package com.example.reclaim.storage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Uploads compressed item images to Firebase Storage.
 * <p>
 * Images are stored at {@code items/{userId}/{uuid}.jpg} and the
 * public download URL is returned for persistence in MongoDB.
 * </p>
 */
public final class ImageUploadService {

    private static final int MAX_IMAGE_DIMENSION = 1280;
    private static final int JPEG_QUALITY = 80;

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
        byte[] compressed = compressImage(context, imageUri);
        if (compressed == null || compressed.length == 0) {
            throw new IOException("Failed to compress image");
        }

        String objectPath = "items/" + userId + "/" + UUID.randomUUID() + ".jpg";
        StorageReference reference = FirebaseStorage.getInstance().getReference().child(objectPath);

        try {
            Task<Uri> uploadTask = reference.putBytes(compressed)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            Exception cause = task.getException();
                            if (cause != null) {
                                throw cause;
                            }
                            throw new IOException("Image upload failed");
                        }
                        return reference.getDownloadUrl();
                    });

            Uri downloadUri = Tasks.await(uploadTask);
            if (downloadUri == null) {
                throw new IOException("Firebase returned an empty download URL");
            }
            return downloadUri.toString();
        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException("Image upload failed: " + e.getMessage(), e);
        }
    }

    @Nullable
    private static byte[] compressImage(@NonNull Context context, @NonNull Uri imageUri)
            throws IOException {
        Bitmap bitmap = decodeSampledBitmap(context, imageUri);
        if (bitmap == null) {
            return null;
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
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream);
        if (scaled != bitmap) {
            scaled.recycle();
        }
        return outputStream.toByteArray();
    }

    @Nullable
    private static Bitmap decodeSampledBitmap(@NonNull Context context, @NonNull Uri imageUri)
            throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;

        try (InputStream boundsStream = context.getContentResolver().openInputStream(imageUri)) {
            if (boundsStream == null) {
                return null;
            }
            BitmapFactory.decodeStream(boundsStream, null, bounds);
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
                return null;
            }
            return BitmapFactory.decodeStream(imageStream, null, options);
        }
    }
}
