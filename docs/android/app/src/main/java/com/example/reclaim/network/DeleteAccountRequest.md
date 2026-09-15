```java
public DeleteAccountRequest(String name, String password) {
    this.name = name;
    this.password = password;
}
```

What that function does: I store the name and password used to confirm account delete.
What the function is responsible for: I am the JSON body for DELETE /api/users/me.
Where it works: I am built in ProfileSettingsFragment and passed to ReClaimApiService.deleteAccount.
Why I chose to write the code of the function the way its written: I keep confirm fields in the body because Retrofit @DELETE cannot carry a body.
More: I use Gson @SerializedName on name and password.
