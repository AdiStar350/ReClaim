```java
@POST("api/auth/login")
Call<LoginResponse> login(@Body LoginRequest loginRequest);
```

What that function does: I send email and password and wait for a token.
What the function is responsible for: I am POST /api/auth/login.
Where it works: I am called from LoginActivity and ResetPasswordActivity after a new password is set.
Why I chose to write the code of the function the way its written: I use @Body so Gson sends LoginRequest as JSON.
More: I use Retrofit @POST, @Body, and Call<LoginResponse>.

```java
@POST("api/auth/register")
Call<com.example.reclaim.model.User> register(@Body RegisterRequest registerRequest);
```

What that function does: I create an account in one step without a code.
What the function is responsible for: I am POST /api/auth/register.
Where it works: I am declared on this service. I did not find a screen that calls me; sign-up uses sendRegistrationOtp.
Why I chose to write the code of the function the way its written: I kept the old register route in case a caller needs it.
More: I use Retrofit @POST and RegisterRequest.

```java
@POST("api/auth/register/send-otp")
Call<Void> sendRegistrationOtp(@Body SendOtpRequest request);
```

What that function does: I send sign-up fields so the server can email a code.
What the function is responsible for: I am POST /api/auth/register/send-otp.
Where it works: I am called from RegisterActivity.
Why I chose to write the code of the function the way its written: I return Void because the code goes to email, not to the app.
More: I use Retrofit @POST and SendOtpRequest.

```java
@POST("api/auth/register/verify-otp")
Call<LoginResponse> verifyRegistrationOtp(@Body VerifyOtpRequest request);
```

What that function does: I check the sign-up code and return a login token.
What the function is responsible for: I am POST /api/auth/register/verify-otp.
Where it works: I am called from VerifyOtpActivity.
Why I chose to write the code of the function the way its written: A LoginResponse lets me save the token the same way as login.
More: I use Retrofit @POST, VerifyOtpRequest, and Call<LoginResponse>.

```java
@POST("api/auth/register/resend-otp")
Call<Void> resendRegistrationOtp(@Body ResendOtpRequest request);
```

What that function does: I ask for a new sign-up code for that email.
What the function is responsible for: I am POST /api/auth/register/resend-otp.
Where it works: I am called from VerifyOtpActivity.
Why I chose to write the code of the function the way its written: I only send email. The server already has the pending sign-up.
More: I use Retrofit @POST and ResendOtpRequest.

```java
@POST("api/auth/forgot-password/send-otp")
Call<Void> sendPasswordResetOtp(@Body ForgotPasswordRequest request);
```

What that function does: I ask the server to email a password-reset code.
What the function is responsible for: I am POST /api/auth/forgot-password/send-otp.
Where it works: I am called from LoginActivity, VerifyOtpActivity, and ProfileSettingsFragment.
Why I chose to write the code of the function the way its written: I return Void so the code never comes back in JSON.
More: I use Retrofit @POST and ForgotPasswordRequest.

```java
@POST("api/auth/forgot-password/verify-otp")
Call<ResetTokenResponse> verifyPasswordResetOtp(@Body VerifyOtpRequest request);
```

What that function does: I check the reset code and return a reset token.
What the function is responsible for: I am POST /api/auth/forgot-password/verify-otp.
Where it works: I am called from VerifyOtpActivity.
Why I chose to write the code of the function the way its written: I reuse VerifyOtpRequest. The reset token is not a login token.
More: I use Retrofit @POST and Call<ResetTokenResponse>.

```java
@POST("api/auth/forgot-password/reset")
Call<Void> resetPassword(@Body ResetPasswordRequest request);
```

What that function does: I set a new password with the reset token.
What the function is responsible for: I am POST /api/auth/forgot-password/reset.
Where it works: I am called from ResetPasswordActivity.
Why I chose to write the code of the function the way its written: I return Void; ResetPasswordActivity then calls login.
More: I use Retrofit @POST and ResetPasswordRequest.

```java
@GET("api/items")
Call<List<Item>> getItems(@Header("Authorization") String authHeader);
```

What that function does: I load all lost and found items.
What the function is responsible for: I am GET /api/items.
Where it works: I am called from ItemsViewModel.
Why I chose to write the code of the function the way its written: I pass the Bearer header so only signed-in users get the list.
More: I use Retrofit @GET, @Header, and List<Item>.

```java
@POST("api/items")
Call<Item> createItem(@Header("Authorization") String authHeader, @Body Item item);
```

What that function does: I post a new lost or found report.
What the function is responsible for: I am POST /api/items.
Where it works: I am called from ReportActivity and ReportSyncWorker.
Why I chose to write the code of the function the way its written: The owner is set on the server from the token, so I send the item body as-is.
More: I use Retrofit @POST, @Header, and @Body Item.

```java
@GET("api/items/my-items")
Call<List<Item>> getMyItems(@Header("Authorization") String authHeader);
```

What that function does: I load items the signed-in user reported.
What the function is responsible for: I am GET /api/items/my-items.
Where it works: I am called from MyItemsFragment, ReportHistoryActivity, and HomeFragment.
Why I chose to write the code of the function the way its written: I keep "my items" on the server so the app does not filter the full list.
More: I use Retrofit @GET and List<Item>.

```java
@GET("api/items/targeted-for-me")
Call<List<Item>> getTargetedFoundReports(@Header("Authorization") String authHeader);
```

What that function does: I load private found reports aimed at this user's lost items.
What the function is responsible for: I am GET /api/items/targeted-for-me.
Where it works: I am called from HomeFragment.
Why I chose to write the code of the function the way its written: Targeted finds are not on the public list, so they need their own call.
More: I use Retrofit @GET and List<Item>.

```java
@GET("api/items/category/{category}")
Call<List<Item>> getItemsByCategory(
        @Header("Authorization") String authHeader,
        @retrofit2.http.Path("category") String category);
```

What that function does: I load items in one category.
What the function is responsible for: I am GET /api/items/category/{category}.
Where it works: I am declared on this service. I did not find a screen that calls me; HomeFragment filters locally.
Why I chose to write the code of the function the way its written: I kept the path in case a later screen wants a server filter.
More: I use Retrofit @Path for the category.

```java
@GET("api/items/{id}")
Call<Item> getItemById(
        @Header("Authorization") String authHeader,
        @retrofit2.http.Path("id") String id);
```

What that function does: I load one item by id.
What the function is responsible for: I am GET /api/items/{id}.
Where it works: I am called from ItemDetailsActivity and ReportActivity when editing.
Why I chose to write the code of the function the way its written: A single-item fetch keeps details fresh without reloading the full list.
More: I use Retrofit @Path("id").

```java
@GET("api/items/{id}/contact")
Call<com.example.reclaim.model.ContactResponse> getLostItemOwnerContact(
        @Header("Authorization") String authHeader,
        @retrofit2.http.Path("id") String id);
```

What that function does: I load the lost-item owner's contact after a match.
What the function is responsible for: I am GET /api/items/{id}/contact.
Where it works: I am called from ItemDetailsActivity.
Why I chose to write the code of the function the way its written: Contact is a separate call so it is not on every item card.
More: I use Retrofit @GET and ContactResponse.

```java
@POST("api/claims")
Call<com.example.reclaim.model.Claim> submitClaim(
        @Header("Authorization") String authHeader,
        @Body ClaimRequest claimRequest);
```

What that function does: I send a claim with the check-question answer.
What the function is responsible for: I am POST /api/claims.
Where it works: I am called from ItemDetailsActivity.
Why I chose to write the code of the function the way its written: I send ClaimRequest so item id and answer travel together.
More: I use Retrofit @POST and Call<Claim>.

```java
@GET("api/claims/my-claims")
Call<java.util.List<com.example.reclaim.model.Claim>> getMyClaims(
        @Header("Authorization") String authHeader);
```

What that function does: I load claims this user made.
What the function is responsible for: I am GET /api/claims/my-claims.
Where it works: I am called from HomeFragment and ReportHistoryActivity.
Why I chose to write the code of the function the way its written: Home uses me to show claim decisions. History uses me for the claims tab.
More: I use Retrofit @GET and List<Claim>.

```java
@GET("api/claims/pending-on-my-items")
Call<java.util.List<com.example.reclaim.model.PendingClaimResponse>> getPendingClaimsOnMyItems(
        @Header("Authorization") String authHeader);
```

What that function does: I load pending claims on items I reported.
What the function is responsible for: I am GET /api/claims/pending-on-my-items.
Where it works: I am called from HomeFragment and PendingClaimsActivity.
Why I chose to write the code of the function the way its written: PendingClaimResponse has claimant name and the check question in one object.
More: I use Retrofit @GET and List<PendingClaimResponse>.

```java
@PUT("api/claims/{claimId}/review")
Call<com.example.reclaim.model.Claim> reviewClaim(
        @Header("Authorization") String authHeader,
        @retrofit2.http.Path("claimId") String claimId,
        @Body ClaimReviewRequest request);
```

What that function does: I approve or reject a claim.
What the function is responsible for: I am PUT /api/claims/{claimId}/review.
Where it works: I am called from PendingClaimsActivity and PendingClaimsSheet.
Why I chose to write the code of the function the way its written: Status is in the body. Claim id is in the path.
More: I use Retrofit @PUT, @Path, and ClaimReviewRequest.

```java
@PUT("api/claims/{claimId}/acknowledge")
Call<com.example.reclaim.model.Claim> acknowledgeClaimDecision(
        @Header("Authorization") String authHeader,
        @retrofit2.http.Path("claimId") String claimId);
```

What that function does: I mark a claim decision as seen.
What the function is responsible for: I am PUT /api/claims/{claimId}/acknowledge.
Where it works: I am called from HomeFragment after the user sees the decision dialog.
Why I chose to write the code of the function the way its written: I send no body so the server only flips the seen flag.
More: I use Retrofit @PUT and @Path.

```java
@GET("api/claims/{claimId}/contact")
Call<com.example.reclaim.model.ContactResponse> getClaimContact(
        @Header("Authorization") String authHeader,
        @retrofit2.http.Path("claimId") String claimId);
```

What that function does: I load contact details after a claim is approved.
What the function is responsible for: I am GET /api/claims/{claimId}/contact.
Where it works: I am called from HomeFragment, ReportHistoryActivity, and PendingClaimsSheet.
Why I chose to write the code of the function the way its written: Contact stays off the claim list until approval.
More: I use Retrofit @GET and ContactResponse.

```java
@GET("api/users/me")
Call<com.example.reclaim.model.User> getCurrentUser(
        @Header("Authorization") String authHeader);
```

What that function does: I load the signed-in user's profile.
What the function is responsible for: I am GET /api/users/me.
Where it works: I am called from ProfileSettingsFragment, EditProfileActivity, and HomeFragment.
Why I chose to write the code of the function the way its written: /me uses the token, so I do not pass a user id.
More: I use Retrofit @GET and Call<User>.

```java
@PUT("api/users/me")
Call<com.example.reclaim.model.User> updateCurrentUser(
        @Header("Authorization") String authHeader,
        @Body UpdateProfileRequest request);
```

What that function does: I save name, email, phone, and picture URL for the signed-in user.
What the function is responsible for: I am PUT /api/users/me.
Where it works: I am called from EditProfileActivity.
Why I chose to write the code of the function the way its written: I return the updated User so the screen can show the new values.
More: I use Retrofit @PUT and UpdateProfileRequest.

```java
@PUT("api/users/me/fcm-token")
Call<Void> updateFcmToken(
        @Header("Authorization") String authHeader,
        @Body FcmTokenRequest request);
```

What that function does: I save this device's push token on the user.
What the function is responsible for: I am PUT /api/users/me/fcm-token.
Where it works: I am called from FcmTokenRegistrar.
Why I chose to write the code of the function the way its written: I return Void because the app only needs a success code.
More: I use Retrofit @PUT and FcmTokenRequest.

```java
@retrofit2.http.HTTP(method = "DELETE", path = "api/users/me", hasBody = true)
Call<Void> deleteAccount(
        @Header("Authorization") String authHeader,
        @Body DeleteAccountRequest request);
```

What that function does: I delete the signed-in account after name and password confirm.
What the function is responsible for: I am DELETE /api/users/me with a body.
Where it works: I am called from ProfileSettingsFragment.
Why I chose to write the code of the function the way its written: Retrofit @DELETE cannot send a body, so I use @HTTP with hasBody true.
More: I use retrofit2.http.HTTP and DeleteAccountRequest.

```java
@PUT("api/items/{id}")
Call<Item> updateItem(
        @Header("Authorization") String authHeader,
        @retrofit2.http.Path("id") String id,
        @Body Item item);
```

What that function does: I save changes to an existing report.
What the function is responsible for: I am PUT /api/items/{id}.
Where it works: I am called from ReportActivity when editing.
Why I chose to write the code of the function the way its written: I send the full Item body and put the id in the path.
More: I use Retrofit @PUT and @Path("id").

```java
@retrofit2.http.DELETE("api/items/{id}")
Call<Void> deleteItem(
        @Header("Authorization") String authHeader,
        @retrofit2.http.Path("id") String id);
```

What that function does: I delete a report by id.
What the function is responsible for: I am DELETE /api/items/{id}.
Where it works: I am called from MyItemsFragment.
Why I chose to write the code of the function the way its written: I return Void because the list reloads after success.
More: I use Retrofit @DELETE and @Path("id").
