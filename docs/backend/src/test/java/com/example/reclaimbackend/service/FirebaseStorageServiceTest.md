# FirebaseStorageServiceTest.java

## Overview
This is a **unit test** class for `FirebaseStorageService.parse` (see `docs/GLOSSARY.md`). It does not call Firebase; it only checks that download URLs are turned into the correct bucket + object path, and that unrelated/blank URLs return null. That parsing is what production delete uses before talking to Storage.

## Concepts used here
- **Firebase Storage** — URLs point at objects in a bucket; see `docs/GLOSSARY.md`.
- **URL parsing** — classic Firebase download URLs and `storage.googleapis.com` object URLs.
- **Unit test** — plain JUnit, no Spring context.

## Libraries and imports
- **JUnit 5** (`@Test`, `assertEquals`, `assertNotNull`, `assertNull`): assertions on parse results.
- **Project**: `FirebaseStorageService` and its package-visible `StorageLocation` / `parse`.

## Fields
*(none — tests call static `parse` directly.)*

## Functions

### `@Test void parse_firebaseDownloadUrl()`
```java
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
```
- **What it does:** checks production URL parsing for a classic Firebase download URL under the primary bucket: `%2F` in the path becomes `/`, yielding `profiles/user-1/abc.jpg`.
- **Where it's called:** by the JUnit test runner.
- **Why it's written this way:** profile pictures use this URL shape after upload.
- **Step by step:**
  1. Build a Firebase download URL for a profile object.
  2. Call `parse`.
  3. Assert bucket and decoded object path.

### `@Test void parse_itemDownloadUrl()`
```java
@Test
void parse_itemDownloadUrl() {
    String url = "https://firebasestorage.googleapis.com/v0/b/"
            + "reclaim-5b4fd.appspot.com/o/items%2Fowner%2Fphoto.jpg?alt=media";

    FirebaseStorageService.StorageLocation location = FirebaseStorageService.parse(url);

    assertNotNull(location);
    assertEquals("reclaim-5b4fd.appspot.com", location.bucket());
    assertEquals("items/owner/photo.jpg", location.objectPath());
}
```
- **What it does:** checks parsing for an item image on the legacy `appspot.com` bucket name.
- **Where it's called:** by the JUnit test runner.
- **Why it's written this way:** older uploads may still use the appspot host/bucket form.
- **Step by step:**
  1. Build a Firebase download URL for an `items/` object.
  2. Parse and assert bucket + path.

### `@Test void parse_googleApisObjectUrl()`
```java
@Test
void parse_googleApisObjectUrl() {
    String url = "https://storage.googleapis.com/reclaim-5b4fd.appspot.com/items/u1/a.jpg";

    FirebaseStorageService.StorageLocation location = FirebaseStorageService.parse(url);

    assertNotNull(location);
    assertEquals("reclaim-5b4fd.appspot.com", location.bucket());
    assertEquals("items/u1/a.jpg", location.objectPath());
}
```
- **What it does:** checks production parsing of a `storage.googleapis.com/{bucket}/{object}` URL.
- **Where it's called:** by the JUnit test runner.
- **Why it's written this way:** covers the non-Firebase-download URL branch in `parse`.
- **Step by step:**
  1. Parse a GCS-style URL.
  2. Assert bucket is the first path segment and the rest is the object path.

### `@Test void parse_rejectsUnrelatedUrl()`
```java
@Test
void parse_rejectsUnrelatedUrl() {
    assertNull(FirebaseStorageService.parse("https://example.com/photo.jpg"));
    assertNull(FirebaseStorageService.parse(""));
    assertNull(FirebaseStorageService.parse(null));
}
```
- **What it does:** checks production safety: non-Firebase URLs and blank/null input return null so deletes are skipped instead of crashing.
- **Where it's called:** by the JUnit test runner.
- **Why it's written this way:** `deleteStoredImage` relies on null parse to no-op.
- **Step by step:**
  1. Parse an unrelated HTTPS URL → null.
  2. Parse empty string → null.
  3. Parse null → null.
