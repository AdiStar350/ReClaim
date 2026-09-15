```java
public User() {
}
```

What that function does: I make an empty user so Gson can fill the fields.
What the function is responsible for: I am the no-arg constructor Gson needs.
Where it works: Gson uses me when ReClaimApiService.getCurrentUser, updateCurrentUser, or register parse JSON.
Why I chose to write the code of the function the way its written: Gson needs an empty constructor before setters run.
More: I leave all fields null until Gson fills them.

```java
public User(String id, String email, String name, String phoneNumber) {
    this.id = id;
    this.email = email;
    this.name = name;
    this.phoneNumber = phoneNumber;
}
```

What that function does: I build a user with the main fields.
What the function is responsible for: I let code make a user without JSON.
Where it works: I did not find an app caller of this constructor. Gson uses the empty one.
Why I chose to write the code of the function the way its written: I kept the four fields screens show most often. Picture URL can be set later.
More: I assign id, email, name, and phoneNumber.

```java
public String getId() {
    return id;
}
```

What that function does: I return the user id.
What the function is responsible for: I let callers know which user this is.
Where it works: Gson fills id. Screens mostly use TokenManager.getUserId instead of this getter.
Why I chose to write the code of the function the way its written: I kept a simple getter for the server id.
More: I use Gson @SerializedName("id").

```java
public String getEmail() {
    return email;
}
```

What that function does: I return the email.
What the function is responsible for: I show the signed-in email on profile screens.
Where it works: I am read by ProfileSettingsFragment and EditProfileActivity.
Why I chose to write the code of the function the way its written: I kept a simple getter.
More: I use Gson @SerializedName("email").

```java
public String getName() {
    return name;
}
```

What that function does: I return the display name.
What the function is responsible for: I show the name on profile screens.
Where it works: I am read by ProfileSettingsFragment and EditProfileActivity.
Why I chose to write the code of the function the way its written: I kept a simple getter.
More: I use Gson @SerializedName("name").

```java
public String getPhoneNumber() {
    return phoneNumber;
}
```

What that function does: I return the phone number.
What the function is responsible for: I fill the phone field on edit profile.
Where it works: I am read by EditProfileActivity.
Why I chose to write the code of the function the way its written: I kept a simple getter.
More: I use Gson @SerializedName("phoneNumber").

```java
public String getProfilePictureUrl() {
    return profilePictureUrl;
}
```

What that function does: I return the profile picture URL.
What the function is responsible for: I let screens load the avatar.
Where it works: I am read by ProfileSettingsFragment, EditProfileActivity, and HomeFragment.
Why I chose to write the code of the function the way its written: I kept a simple getter. The URL may be empty.
More: I use Gson @SerializedName("profilePictureUrl").

```java
public void setId(String id) {
    this.id = id;
}
```

What that function does: I set the user id.
What the function is responsible for: I let Gson write the id.
Where it works: Gson may call me while parsing User JSON.
Why I chose to write the code of the function the way its written: I kept a setter because the field is not final.
More: I assign the id field.

```java
public void setEmail(String email) {
    this.email = email;
}
```

What that function does: I set the email.
What the function is responsible for: I let Gson write the email.
Where it works: Gson may call me while parsing User JSON.
Why I chose to write the code of the function the way its written: I kept a setter because the field is not final.
More: I assign the email field.

```java
public void setName(String name) {
    this.name = name;
}
```

What that function does: I set the display name.
What the function is responsible for: I let Gson write the name.
Where it works: Gson may call me while parsing User JSON.
Why I chose to write the code of the function the way its written: I kept a setter because the field is not final.
More: I assign the name field.

```java
public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
}
```

What that function does: I set the phone number.
What the function is responsible for: I let Gson write the phone.
Where it works: Gson may call me while parsing User JSON.
Why I chose to write the code of the function the way its written: I kept a setter because the field is not final.
More: I assign the phoneNumber field.

```java
public void setProfilePictureUrl(String profilePictureUrl) {
    this.profilePictureUrl = profilePictureUrl;
}
```

What that function does: I set the profile picture URL.
What the function is responsible for: I let Gson write the picture URL.
Where it works: Gson may call me while parsing User JSON.
Why I chose to write the code of the function the way its written: I kept a setter because the field is not final.
More: I assign the profilePictureUrl field.
