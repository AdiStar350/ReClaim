package com.example.reclaimbackend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class FirebaseStorageServiceTest {

    @Test
    void parse_firebaseDownloadUrl() {
        String url = "https://firebasestorage.googleapis.com/v0/b/"
                + "reclaim-5b4fd.firebasestorage.app/o/"
                + "profiles%2Fuser-1%2Fabc.jpg?alt=media&token=tok";

        FirebaseStorageService.StorageLocation location = FirebaseStorageService.parse(url);

        assertNotNull(location);
        assertEquals("reclaim-5b4fd.firebasestorage.app", location.bucket());
        assertEquals("profiles/user-1/abc.jpg", location.objectPath());
    }

    @Test
    void parse_itemDownloadUrl() {
        String url = "https://firebasestorage.googleapis.com/v0/b/"
                + "reclaim-5b4fd.appspot.com/o/items%2Fowner%2Fphoto.jpg?alt=media";

        FirebaseStorageService.StorageLocation location = FirebaseStorageService.parse(url);

        assertNotNull(location);
        assertEquals("reclaim-5b4fd.appspot.com", location.bucket());
        assertEquals("items/owner/photo.jpg", location.objectPath());
    }

    @Test
    void parse_googleApisObjectUrl() {
        String url = "https://storage.googleapis.com/reclaim-5b4fd.appspot.com/items/u1/a.jpg";

        FirebaseStorageService.StorageLocation location = FirebaseStorageService.parse(url);

        assertNotNull(location);
        assertEquals("reclaim-5b4fd.appspot.com", location.bucket());
        assertEquals("items/u1/a.jpg", location.objectPath());
    }

    @Test
    void parse_rejectsUnrelatedUrl() {
        assertNull(FirebaseStorageService.parse("https://example.com/photo.jpg"));
        assertNull(FirebaseStorageService.parse(""));
        assertNull(FirebaseStorageService.parse(null));
    }
}
