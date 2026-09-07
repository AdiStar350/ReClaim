package com.example.reclaim.network;

import com.example.reclaim.model.Item;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;

/**
 * HTTP calls to the backend.
 * Signed-in calls need {@code Authorization: Bearer <token>}.
 */
public interface ReClaimApiService {

    // ──
    // Auth
    // ──

    /**
     * Signs in with email and password.
     *
     * @param loginRequest email and password
     * @return the login response
     */
    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    /**
     * Creates a new account.
     *
     * @param registerRequest sign-up fields
     * @return the new user
     */
    @POST("api/auth/register")
    Call<com.example.reclaim.model.User> register(@Body RegisterRequest registerRequest);

    /**
     * Sends a sign-up code to the given email.
     */
    @POST("api/auth/register/send-otp")
    Call<Void> sendRegistrationOtp(@Body SendOtpRequest request);

    /**
     * Checks the sign-up code and signs the user in.
     */
    @POST("api/auth/register/verify-otp")
    Call<LoginResponse> verifyRegistrationOtp(@Body VerifyOtpRequest request);

    /**
     * Sends a new sign-up code.
     */
    @POST("api/auth/register/resend-otp")
    Call<Void> resendRegistrationOtp(@Body ResendOtpRequest request);

    // Forgot password

    /**
     * Sends a password-reset code.
     */
    @POST("api/auth/forgot-password/send-otp")
    Call<Void> sendPasswordResetOtp(@Body ForgotPasswordRequest request);

    /**
     * Checks the reset code and returns a reset token.
     */
    @POST("api/auth/forgot-password/verify-otp")
    Call<ResetTokenResponse> verifyPasswordResetOtp(@Body VerifyOtpRequest request);

    /**
     * Sets a new password with the reset token.
     */
    @POST("api/auth/forgot-password/reset")
    Call<Void> resetPassword(@Body ResetPasswordRequest request);

    // ──
    // Items
    // ──

    /**
     * All lost and found items.
     *
     * @param authHeader {@code "Bearer <token>"}
     * @return the item list
     */
    @GET("api/items")
    Call<List<Item>> getItems(@Header("Authorization") String authHeader);

    /**
     * Posts a new item.
     *
     * @param authHeader {@code "Bearer <token>"}
     * @param item       the item (owner is set on the server)
     * @return the created item
     */
    @POST("api/items")
    Call<Item> createItem(@Header("Authorization") String authHeader, @Body Item item);

    /**
     * Items the signed-in user reported.
     */
    @GET("api/items/my-items")
    Call<List<Item>> getMyItems(@Header("Authorization") String authHeader);

    /**
     * Items in one category.
     */
    @GET("api/items/category/{category}")
    Call<List<Item>> getItemsByCategory(
            @Header("Authorization") String authHeader,
            @retrofit2.http.Path("category") String category);

    /**
     * One item by id.
     */
    @GET("api/items/{id}")
    Call<Item> getItemById(
            @Header("Authorization") String authHeader,
            @retrofit2.http.Path("id") String id);

    @GET("api/items/{id}/contact")
    Call<com.example.reclaim.model.ContactResponse> getLostItemOwnerContact(
            @Header("Authorization") String authHeader,
            @retrofit2.http.Path("id") String id);

    // ──
    // Claims
    // ──

    /**
     * Sends a claim on an item.
     *
     * @param authHeader   {@code "Bearer <token>"}
     * @param claimRequest item id and answer
     * @return the created claim
     */
    @POST("api/claims")
    Call<com.example.reclaim.model.Claim> submitClaim(
            @Header("Authorization") String authHeader,
            @Body ClaimRequest claimRequest);

    @GET("api/claims/my-claims")
    Call<java.util.List<com.example.reclaim.model.Claim>> getMyClaims(
            @Header("Authorization") String authHeader);

    @GET("api/claims/pending-on-my-items")
    Call<java.util.List<com.example.reclaim.model.PendingClaimResponse>> getPendingClaimsOnMyItems(
            @Header("Authorization") String authHeader);

    @PUT("api/claims/{claimId}/review")
    Call<com.example.reclaim.model.Claim> reviewClaim(
            @Header("Authorization") String authHeader,
            @retrofit2.http.Path("claimId") String claimId,
            @Body ClaimReviewRequest request);

    @PUT("api/claims/{claimId}/acknowledge")
    Call<com.example.reclaim.model.Claim> acknowledgeClaimDecision(
            @Header("Authorization") String authHeader,
            @retrofit2.http.Path("claimId") String claimId);

    @GET("api/claims/{claimId}/contact")
    Call<com.example.reclaim.model.ContactResponse> getClaimContact(
            @Header("Authorization") String authHeader,
            @retrofit2.http.Path("claimId") String claimId);

    // ──
    // Users
    // ──

    @GET("api/users/me")
    Call<com.example.reclaim.model.User> getCurrentUser(
            @Header("Authorization") String authHeader);

    @PUT("api/users/me")
    Call<com.example.reclaim.model.User> updateCurrentUser(
            @Header("Authorization") String authHeader,
            @Body UpdateProfileRequest request);

    /**
     * Saves this device's push token.
     */
    @PUT("api/users/me/fcm-token")
    Call<Void> updateFcmToken(
            @Header("Authorization") String authHeader,
            @Body FcmTokenRequest request);

    // @DELETE cannot carry a body, so @HTTP sends
    // the confirm fields with the delete.
    @retrofit2.http.HTTP(method = "DELETE", path = "api/users/me", hasBody = true)
    Call<Void> deleteAccount(
            @Header("Authorization") String authHeader,
            @Body DeleteAccountRequest request);

    @PUT("api/items/{id}")
    Call<Item> updateItem(
            @Header("Authorization") String authHeader,
            @retrofit2.http.Path("id") String id,
            @Body Item item);

    @retrofit2.http.DELETE("api/items/{id}")
    Call<Void> deleteItem(
            @Header("Authorization") String authHeader,
            @retrofit2.http.Path("id") String id);
}
