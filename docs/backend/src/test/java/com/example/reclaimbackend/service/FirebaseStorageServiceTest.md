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

What that function does: I check that a firebasestorage.googleapis.com profile URL parses to bucket and decoded path.
What the function is responsible for: I test FirebaseStorageService.parse on the encoded download URL.
Where it works: I call FirebaseStorageService.parse.
Why I chose to write the code of the function the way its written: I wrote it this way because %2F in the path should become / after decode. I use a real-looking bucket name.
More: I use assertNotNull, assertEquals, and StorageLocation.bucket/objectPath.

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

What that function does: I check that a legacy appspot download URL for an item photo parses.
What the function is responsible for: I test FirebaseStorageService.parse on items/ with the old bucket name.
Where it works: I call FirebaseStorageService.parse.
Why I chose to write the code of the function the way its written: I wrote it this way because old links used appspot.com, so parse must still read them.
More: I use the same parse and assertions as the profile test.

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

What that function does: I check that a storage.googleapis.com object URL splits bucket and path.
What the function is responsible for: I test FirebaseStorageService.parse on the host/path form.
Where it works: I call FirebaseStorageService.parse.
Why I chose to write the code of the function the way its written: I wrote it this way because this URL has no /o/ encoding, so the URI branch must work.
More: I use URI parsing inside parse, asserted through bucket and objectPath.

```java
@Test
void parse_rejectsUnrelatedUrl() {
    assertNull(FirebaseStorageService.parse("https://example.com/photo.jpg"));
    assertNull(FirebaseStorageService.parse(""));
    assertNull(FirebaseStorageService.parse(null));
}
```

What that function does: I check that unrelated, empty, and null URLs return null.
What the function is responsible for: I test FirebaseStorageService.parse's skip path.
Where it works: I call FirebaseStorageService.parse three times.
Why I chose to write the code of the function the way its written: I wrote it this way because deleteStoredImage skips null parse results, so these cases should not look like Storage objects.
More: I use assertNull.
