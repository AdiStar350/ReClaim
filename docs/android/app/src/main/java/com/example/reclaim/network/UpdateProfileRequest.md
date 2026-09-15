```java
public UpdateProfileRequest(String name, String email, String phoneNumber) {
    this(name, email, phoneNumber, null);
}
```

What that function does: I build a profile update with no picture URL.
What the function is responsible for: I forward to the four-arg constructor with a null picture.
Where it works: I did not find an app caller of this three-arg constructor. EditProfileActivity uses the four-arg one.
Why I chose to write the code of the function the way its written: I reuse the other constructor so fields stay in one place.
More: I pass null for profilePictureUrl.

```java
public UpdateProfileRequest(String name, String email, String phoneNumber,
                            String profilePictureUrl) {
    this.name = name;
    this.email = email;
    this.phoneNumber = phoneNumber;
    this.profilePictureUrl = profilePictureUrl;
}
```

What that function does: I store name, email, phone, and optional picture URL for a profile update.
What the function is responsible for: I am the JSON body for PUT /api/users/me.
Where it works: I am built in EditProfileActivity and passed to ReClaimApiService.updateCurrentUser.
Why I chose to write the code of the function the way its written: I allow a null picture URL so a save without a new photo still works.
More: I use Gson @SerializedName on name, email, phoneNumber, and profilePictureUrl.
