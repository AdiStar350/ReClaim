# ReClaimApiService.java

## Overview
This file is a **Retrofit API interface**. A Retrofit API interface is a plain Java
`interface` where each method describes one HTTP call to the backend: the HTTP verb,
the URL path, what you send, and what you get back. There are no method bodies. At
runtime Retrofit reads the annotations on each method and writes the real networking
code for you. See `docs/GLOSSARY.md` for HTTP, endpoint, REST, Retrofit, and
`Call`/`enqueue`.

Think of this interface as the single, typed list of "doors" the ReClaim server
opens. Every screen that needs the network gets an instance of this interface from
`RetrofitClient.getApiService()` and calls these methods instead of building URLs by
hand.

## Concepts used here
- **Retrofit.** Turns each annotated method into an HTTP request and parses the JSON
  reply into a Java object; see `docs/GLOSSARY.md`.
- **`Call<T>`.** One pending request that will return a `T`. You run it with
  `enqueue` (background) or `execute` (blocking); see `docs/GLOSSARY.md`.
- **Retrofit HTTP annotations.** `@POST`, `@GET`, `@PUT`, `@DELETE`, and `@HTTP` set
  the HTTP verb and path.
- **`@Body`.** Marks the method argument that Retrofit turns into the JSON request
  body (usually one of the request DTO classes).
- **`@Path`.** Fills a `{placeholder}` in the URL with a method argument.
- **`@Header`.** Sends a request header; here it carries the login token as
  `Authorization: Bearer <token>`; see JWT in `docs/GLOSSARY.md`.
- **DTO (Data Transfer Object).** The small request/response classes named below
  (`LoginRequest`, `LoginResponse`, etc.); see `docs/GLOSSARY.md`.

## Libraries and imports
- **Retrofit (`retrofit2.Call`, `retrofit2.http.*`).** `Call` is the return type of
  every method. The `http` package gives the annotations used here: `@GET`, `@POST`,
  `@PUT`, `@Header`, `@Body`, and (written with their full names inline)
  `@retrofit2.http.Path`, `@retrofit2.http.DELETE`, and `@retrofit2.http.HTTP`.
- **Project classes.** `com.example.reclaim.model.Item` is imported; other model and
  network types (`User`, `Claim`, `ContactResponse`, `PendingClaimResponse`, and the
  request DTOs) are referenced by their full names in the signatures.
- **Java standard.** `java.util.List` is used for the endpoints that return many
  items or claims.

## How Retrofit turns these signatures into real calls
You never write a class that `implements` this interface. Instead
`RetrofitClient.getInstance().create(ReClaimApiService.class)` asks Retrofit to build
a hidden object (a *dynamic proxy*) that does implement it. When you call, say,
`login(request)`, that proxy reads the `@POST("api/auth/login")` annotation, turns the
`@Body` argument into JSON with Gson, sends the HTTP request through OkHttp, and wraps
the pending work in a `Call<LoginResponse>`. See Retrofit in `docs/GLOSSARY.md`.

## Functions
This is an interface, so there are no helper methods and no bodies. Each entry below
is one endpoint: the signature, the HTTP verb and path, what it sends, what it returns,
and where the app calls it.

### `Call<LoginResponse> login(@Body LoginRequest loginRequest)`
```java
@POST("api/auth/login")
Call<LoginResponse> login(@Body LoginRequest loginRequest);
```
- **HTTP:** `POST api/auth/login`.
- **Sends / returns:** sends a `LoginRequest` (email + password); returns a
  `LoginResponse` holding the login token.
- **Where it's called:** `LoginActivity.attemptLogin` and, right after a password
  reset, `ResetPasswordActivity`.

### `Call<User> register(@Body RegisterRequest registerRequest)`
```java
@POST("api/auth/register")
Call<com.example.reclaim.model.User> register(@Body RegisterRequest registerRequest);
```
- **HTTP:** `POST api/auth/register`.
- **Sends / returns:** sends a `RegisterRequest`; returns the new `User`.
- **Where it's called:** not called from app code right now. Sign-up goes through the
  OTP flow instead (`sendRegistrationOtp` then `verifyRegistrationOtp`), so this
  direct-register door exists but is currently unused by the Android client.

### `Call<Void> sendRegistrationOtp(@Body SendOtpRequest request)`
```java
@POST("api/auth/register/send-otp")
Call<Void> sendRegistrationOtp(@Body SendOtpRequest request);
```
- **HTTP:** `POST api/auth/register/send-otp`.
- **Sends / returns:** sends all sign-up fields inside a `SendOtpRequest`; returns
  nothing (`Void`) beyond a success status. The account is created only after the code
  is verified.
- **Where it's called:** `RegisterActivity` after the form is filled in.

### `Call<LoginResponse> verifyRegistrationOtp(@Body VerifyOtpRequest request)`
```java
@POST("api/auth/register/verify-otp")
Call<LoginResponse> verifyRegistrationOtp(@Body VerifyOtpRequest request);
```
- **HTTP:** `POST api/auth/register/verify-otp`.
- **Sends / returns:** sends email + 6-digit code (`VerifyOtpRequest`); returns a
  `LoginResponse` token so the user is signed in immediately.
- **Where it's called:** `VerifyOtpActivity` in sign-up mode.

### `Call<Void> resendRegistrationOtp(@Body ResendOtpRequest request)`
```java
@POST("api/auth/register/resend-otp")
Call<Void> resendRegistrationOtp(@Body ResendOtpRequest request);
```
- **HTTP:** `POST api/auth/register/resend-otp`.
- **Sends / returns:** sends the email (`ResendOtpRequest`); returns nothing.
- **Where it's called:** `VerifyOtpActivity` when the user taps "resend" in sign-up
  mode.

### `Call<Void> sendPasswordResetOtp(@Body ForgotPasswordRequest request)`
```java
@POST("api/auth/forgot-password/send-otp")
Call<Void> sendPasswordResetOtp(@Body ForgotPasswordRequest request);
```
- **HTTP:** `POST api/auth/forgot-password/send-otp`.
- **Sends / returns:** sends the email (`ForgotPasswordRequest`); returns nothing.
- **Where it's called:** `LoginActivity` (forgot-password dialog),
  `ProfileSettingsFragment` (change-password), and `VerifyOtpActivity` (resend in
  reset mode).

### `Call<ResetTokenResponse> verifyPasswordResetOtp(@Body VerifyOtpRequest request)`
```java
@POST("api/auth/forgot-password/verify-otp")
Call<ResetTokenResponse> verifyPasswordResetOtp(@Body VerifyOtpRequest request);
```
- **HTTP:** `POST api/auth/forgot-password/verify-otp`.
- **Sends / returns:** sends email + code; returns a `ResetTokenResponse` whose reset
  token is needed to set a new password.
- **Where it's called:** `VerifyOtpActivity` in reset mode.

### `Call<Void> resetPassword(@Body ResetPasswordRequest request)`
```java
@POST("api/auth/forgot-password/reset")
Call<Void> resetPassword(@Body ResetPasswordRequest request);
```
- **HTTP:** `POST api/auth/forgot-password/reset`.
- **Sends / returns:** sends email + reset token + new password
  (`ResetPasswordRequest`); returns nothing.
- **Where it's called:** `ResetPasswordActivity`.

### `Call<List<Item>> getItems(@Header("Authorization") String authHeader)`
```java
@GET("api/items")
Call<List<Item>> getItems(@Header("Authorization") String authHeader);
```
- **HTTP:** `GET api/items`.
- **Sends / returns:** sends the auth header only; returns the full list of lost and
  found `Item`s.
- **Where it's called:** `ItemsViewModel.loadItems`, which feeds the home and search
  screens.

### `Call<Item> createItem(@Header("Authorization") String authHeader, @Body Item item)`
```java
@POST("api/items")
Call<Item> createItem(@Header("Authorization") String authHeader, @Body Item item);
```
- **HTTP:** `POST api/items`.
- **Sends / returns:** sends the new `Item`; returns the created `Item` (with the
  server-assigned id and owner).
- **Where it's called:** `ReportActivity` when submitting a new report, and
  `ReportSyncWorker` when sending a report that was queued while offline.

### `Call<List<Item>> getMyItems(@Header("Authorization") String authHeader)`
```java
@GET("api/items/my-items")
Call<List<Item>> getMyItems(@Header("Authorization") String authHeader);
```
- **HTTP:** `GET api/items/my-items`.
- **Sends / returns:** returns only the items the signed-in user reported.
- **Where it's called:** `MyItemsFragment`, `ReportHistoryActivity`, and
  `HomeFragment`.

### `Call<List<Item>> getTargetedFoundReports(@Header("Authorization") String authHeader)`
```java
@GET("api/items/targeted-for-me")
Call<List<Item>> getTargetedFoundReports(@Header("Authorization") String authHeader);
```
- **HTTP:** `GET api/items/targeted-for-me`.
- **Sends / returns:** returns private "found" reports aimed at this user's lost
  items.
- **Where it's called:** `HomeFragment` when building the matches section.

### `Call<List<Item>> getItemsByCategory(...)`
```java
@GET("api/items/category/{category}")
Call<List<Item>> getItemsByCategory(
        @Header("Authorization") String authHeader,
        @retrofit2.http.Path("category") String category);
```
- **HTTP:** `GET api/items/category/{category}` (the `{category}` placeholder is
  filled by the `@Path` argument).
- **Sends / returns:** returns items in one category.
- **Where it's called:** not called from app code right now; category filtering is
  done on the client from the full list, so this endpoint is currently unused.

### `Call<Item> getItemById(...)`
```java
@GET("api/items/{id}")
Call<Item> getItemById(
        @Header("Authorization") String authHeader,
        @retrofit2.http.Path("id") String id);
```
- **HTTP:** `GET api/items/{id}`.
- **Sends / returns:** returns one `Item` by its id.
- **Where it's called:** `ItemDetailsActivity` (to refresh details) and
  `ReportActivity` (to load an item being edited).

### `Call<ContactResponse> getLostItemOwnerContact(...)`
```java
@GET("api/items/{id}/contact")
Call<com.example.reclaim.model.ContactResponse> getLostItemOwnerContact(
        @Header("Authorization") String authHeader,
        @retrofit2.http.Path("id") String id);
```
- **HTTP:** `GET api/items/{id}/contact`.
- **Sends / returns:** returns the item owner's `ContactResponse` (name, email,
  phone), given the item id.
- **Where it's called:** `ItemDetailsActivity` when the finder wants to reach the
  owner of a lost item.

### `Call<Claim> submitClaim(...)`
```java
@POST("api/claims")
Call<com.example.reclaim.model.Claim> submitClaim(
        @Header("Authorization") String authHeader,
        @Body ClaimRequest claimRequest);
```
- **HTTP:** `POST api/claims`.
- **Sends / returns:** sends a `ClaimRequest` (item id + answer to the check
  question); returns the created `Claim`.
- **Where it's called:** `ItemDetailsActivity` when the user claims an item.

### `Call<List<Claim>> getMyClaims(@Header("Authorization") String authHeader)`
```java
@GET("api/claims/my-claims")
Call<java.util.List<com.example.reclaim.model.Claim>> getMyClaims(
        @Header("Authorization") String authHeader);
```
- **HTTP:** `GET api/claims/my-claims`.
- **Sends / returns:** returns the claims the signed-in user made.
- **Where it's called:** `HomeFragment` and `ReportHistoryActivity`.

### `Call<List<PendingClaimResponse>> getPendingClaimsOnMyItems(...)`
```java
@GET("api/claims/pending-on-my-items")
Call<java.util.List<com.example.reclaim.model.PendingClaimResponse>> getPendingClaimsOnMyItems(
        @Header("Authorization") String authHeader);
```
- **HTTP:** `GET api/claims/pending-on-my-items`.
- **Sends / returns:** returns claims other people made on this user's items, using
  the richer `PendingClaimResponse` shape (so the owner sees claimant name, item
  title, the check question, etc.).
- **Where it's called:** `HomeFragment` and `PendingClaimsActivity`.

### `Call<Claim> reviewClaim(...)`
```java
@PUT("api/claims/{claimId}/review")
Call<com.example.reclaim.model.Claim> reviewClaim(
        @Header("Authorization") String authHeader,
        @retrofit2.http.Path("claimId") String claimId,
        @Body ClaimReviewRequest request);
```
- **HTTP:** `PUT api/claims/{claimId}/review`.
- **Sends / returns:** sends a `ClaimReviewRequest` (the new status, e.g. approved or
  rejected); returns the updated `Claim`.
- **Where it's called:** `PendingClaimsActivity` and `PendingClaimsSheet` when the
  owner approves or rejects a claim.

### `Call<Claim> acknowledgeClaimDecision(...)`
```java
@PUT("api/claims/{claimId}/acknowledge")
Call<com.example.reclaim.model.Claim> acknowledgeClaimDecision(
        @Header("Authorization") String authHeader,
        @retrofit2.http.Path("claimId") String claimId);
```
- **HTTP:** `PUT api/claims/{claimId}/acknowledge`.
- **Sends / returns:** no body; returns the updated `Claim`. It marks that the
  claimant has seen the approve/reject decision so it stops re-appearing.
- **Where it's called:** `HomeFragment` after the claimant reads the result popup.

### `Call<ContactResponse> getClaimContact(...)`
```java
@GET("api/claims/{claimId}/contact")
Call<com.example.reclaim.model.ContactResponse> getClaimContact(
        @Header("Authorization") String authHeader,
        @retrofit2.http.Path("claimId") String claimId);
```
- **HTTP:** `GET api/claims/{claimId}/contact`.
- **Sends / returns:** returns the other party's `ContactResponse` for an approved
  claim, so the two people can meet up.
- **Where it's called:** `HomeFragment`, `PendingClaimsSheet`, and
  `ReportHistoryActivity`.

### `Call<User> getCurrentUser(@Header("Authorization") String authHeader)`
```java
@GET("api/users/me")
Call<com.example.reclaim.model.User> getCurrentUser(
        @Header("Authorization") String authHeader);
```
- **HTTP:** `GET api/users/me`.
- **Sends / returns:** returns the signed-in `User`'s profile.
- **Where it's called:** `HomeFragment`, `EditProfileActivity`, and
  `ProfileSettingsFragment`.

### `Call<User> updateCurrentUser(...)`
```java
@PUT("api/users/me")
Call<com.example.reclaim.model.User> updateCurrentUser(
        @Header("Authorization") String authHeader,
        @Body UpdateProfileRequest request);
```
- **HTTP:** `PUT api/users/me`.
- **Sends / returns:** sends an `UpdateProfileRequest` (name, email, phone, optional
  photo URL); returns the updated `User`.
- **Where it's called:** `EditProfileActivity`.

### `Call<Void> updateFcmToken(...)`
```java
@PUT("api/users/me/fcm-token")
Call<Void> updateFcmToken(
        @Header("Authorization") String authHeader,
        @Body FcmTokenRequest request);
```
- **HTTP:** `PUT api/users/me/fcm-token`.
- **Sends / returns:** sends this device's push token (`FcmTokenRequest`); returns
  nothing. This lets the backend send push notifications to the device.
- **Where it's called:** `FcmTokenRegistrar.sendToken`.

### `Call<Void> deleteAccount(...)`
```java
// @DELETE cannot carry a body, so @HTTP sends
// the confirm fields with the delete.
@retrofit2.http.HTTP(method = "DELETE", path = "api/users/me", hasBody = true)
Call<Void> deleteAccount(
        @Header("Authorization") String authHeader,
        @Body DeleteAccountRequest request);
```
- **HTTP:** `DELETE api/users/me`, sent through `@HTTP` with `hasBody = true`.
- **Sends / returns:** sends a `DeleteAccountRequest` (name + password to confirm);
  returns nothing.
- **Why `@HTTP` instead of `@DELETE`:** Retrofit's plain `@DELETE` does not allow a
  request body, but this endpoint needs the confirm fields in the body, so the more
  general `@HTTP` annotation is used with `hasBody = true`.
- **Where it's called:** `ProfileSettingsFragment` when deleting the account.

### `Call<Item> updateItem(...)`
```java
@PUT("api/items/{id}")
Call<Item> updateItem(
        @Header("Authorization") String authHeader,
        @retrofit2.http.Path("id") String id,
        @Body Item item);
```
- **HTTP:** `PUT api/items/{id}`.
- **Sends / returns:** sends the edited `Item`; returns the updated `Item`.
- **Where it's called:** `ReportActivity` when editing an existing report.

### `Call<Void> deleteItem(...)`
```java
@retrofit2.http.DELETE("api/items/{id}")
Call<Void> deleteItem(
        @Header("Authorization") String authHeader,
        @retrofit2.http.Path("id") String id);
```
- **HTTP:** `DELETE api/items/{id}`.
- **Sends / returns:** no body; returns nothing. Removes one of the user's items.
- **Where it's called:** `MyItemsFragment` when the user deletes their report.
