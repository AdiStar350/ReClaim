```java
public String getName() {
    return name;
}
```

What that function does: I return the other person's name.
What the function is responsible for: I show who to contact after a match or approved claim.
Where it works: I am read by ContactInfoDialog after getClaimContact or getLostItemOwnerContact.
Why I chose to write the code of the function the way its written: I kept a simple getter. Gson fills the field.
More: I use Gson @SerializedName("name").

```java
public String getEmail() {
    return email;
}
```

What that function does: I return the other person's email.
What the function is responsible for: I show email in the contact dialog.
Where it works: I am read by ContactInfoDialog.
Why I chose to write the code of the function the way its written: I kept a simple getter.
More: I use Gson @SerializedName("email").

```java
public String getPhoneNumber() {
    return phoneNumber;
}
```

What that function does: I return the other person's phone.
What the function is responsible for: I show phone in the contact dialog.
Where it works: I am read by ContactInfoDialog.
Why I chose to write the code of the function the way its written: I kept a simple getter.
More: I use Gson @SerializedName("phoneNumber").
