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
}
