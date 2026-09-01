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
 * Retrofit API service interface for the ReClaim backend.
 * <p>
 * Defines the HTTP endpoints the app communicates with. Each method
 * returns a {@link Call} wrapper for asynchronous execution via
 * {@link Call#enqueue}.
 * </p>
 *
 * <h3>Authentication:</h3>
 * All authenticated endpoints require an {@code Authorization} header
 * in the format {@code Bearer <token>}. The token is obtained from the
 * login endpoint and stored in {@link android.content.SharedPreferences}.
 */
public interface ReClaimApiService {

    // ═════════════════════════════════════════════════════════════════════
    //  AUTHENTICATION
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Authenticates a user with email and password.
     * <p>
     * On success, the server returns a {@link LoginResponse} containing
     * a JWT access token.
     * </p>
     *
     * @param loginRequest the login credentials
     * @return a {@link Call} wrapping the {@link LoginResponse}
     */
    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    /**
     * Registers a new user account.
     * <p>
     * On success, the server returns the created {@link com.example.reclaim.model.User}
     * object (without the password field).
     * </p>
     *
     * @param registerRequest the registration data
     * @return a {@link Call} wrapping the created user
     */
    @POST("api/auth/register")
    Call<com.example.reclaim.model.User> register(@Body RegisterRequest registerRequest);

    /**
     * Step 1 of OTP registration: stores the payload server-side and emails a
     * 6-digit verification code to the address provided.
     */
    @POST("api/auth/register/send-otp")
    Call<Void> sendRegistrationOtp(@Body SendOtpRequest request);

    /**
     * Step 2 of OTP registration: validates the code, creates the user in
     * MongoDB, and returns a JWT so the client is logged in immediately.
     */
    @POST("api/auth/register/verify-otp")
    Call<LoginResponse> verifyRegistrationOtp(@Body VerifyOtpRequest request);

    /**
     * Re-sends a fresh OTP for a pending registration.
     */
    @POST("api/auth/register/resend-otp")
    Call<Void> resendRegistrationOtp(@Body ResendOtpRequest request);

    // ─── Forgot password ───────────────────────────────────────────────

    /**
     * Step 1 of the forgot-password flow: emails a 6-digit reset code.
     */
    @POST("api/auth/forgot-password/send-otp")
    Call<Void> sendPasswordResetOtp(@Body ForgotPasswordRequest request);

    /**
     * Step 2: validates the reset code and returns an opaque reset token.
     */
    @POST("api/auth/forgot-password/verify-otp")
    Call<ResetTokenResponse> verifyPasswordResetOtp(@Body VerifyOtpRequest request);

    /**
     * Step 3: sets the new password using the reset token.
     */
    @POST("api/auth/forgot-password/reset")
    Call<Void> resetPassword(@Body ResetPasswordRequest request);

    // ═════════════════════════════════════════════════════════════════════
    //  ITEMS
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Retrieves the list of all reported lost and found items.
     * <p>
     * Requires a valid JWT token in the {@code Authorization} header.
     * </p>
     *
     * @param authHeader the authorization header value, e.g.
     *                   {@code "Bearer eyJhbGciOi..."}
     * @return a {@link Call} wrapping a list of {@link Item} objects
     */
    @GET("api/items")
    Call<List<Item>> getItems(@Header("Authorization") String authHeader);

    /**
     * Creates a new lost or found item report.
     *
     * @param authHeader the authorization header ({@code "Bearer <token>"})
     * @param item       the item payload (ownerId is set server-side)
     * @return a {@link Call} wrapping the created {@link Item}
     */
    @POST("api/items")
    Call<Item> createItem(@Header("Authorization") String authHeader, @Body Item item);

    /**
     * Retrieves items reported by the authenticated user.
     */
    @GET("api/items/my-items")
    Call<List<Item>> getMyItems(@Header("Authorization") String authHeader);

    /**
     * Retrieves items filtered by category.
     */
    @GET("api/items/category/{category}")
    Call<List<Item>> getItemsByCategory(
            @Header("Authorization") String authHeader,
            @retrofit2.http.Path("category") String category);

    /**
     * Retrieves a single item by ID.
     */
    @GET("api/items/{id}")
    Call<Item> getItemById(
            @Header("Authorization") String authHeader,
            @retrofit2.http.Path("id") String id);

    @GET("api/items/{id}/contact")
    Call<com.example.reclaim.model.ContactResponse> getLostItemOwnerContact(
            @Header("Authorization") String authHeader,
            @retrofit2.http.Path("id") String id);

    // ═════════════════════════════════════════════════════════════════════
    //  CLAIMS
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Submits an ownership claim on an item.
     * <p>
     * The server extracts the {@code claimantId} from the JWT token
     * and sets the initial claim status to {@code PENDING}.
     * </p>
     *
     * @param authHeader   the authorization header ({@code "Bearer <token>"})
     * @param claimRequest the claim data (itemId + validationAnswer)
     * @return a {@link Call} wrapping the created {@link com.example.reclaim.model.Claim}
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

    // ═════════════════════════════════════════════════════════════════════
    //  USERS
    // ═════════════════════════════════════════════════════════════════════

    @GET("api/users/me")
    Call<com.example.reclaim.model.User> getCurrentUser(
            @Header("Authorization") String authHeader);

    @PUT("api/users/me")
    Call<com.example.reclaim.model.User> updateCurrentUser(
            @Header("Authorization") String authHeader,
            @Body UpdateProfileRequest request);

    /**
     * Registers the device's FCM token so the backend can send push
     * notifications (item matches, claim reviews) to this user.
     */
    @PUT("api/users/me/fcm-token")
    Call<Void> updateFcmToken(
            @Header("Authorization") String authHeader,
            @Body FcmTokenRequest request);

    // Retrofit's @DELETE forbids a request body, so use @HTTP with hasBody=true
    // to send the confirmation credentials in the DELETE body.
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
