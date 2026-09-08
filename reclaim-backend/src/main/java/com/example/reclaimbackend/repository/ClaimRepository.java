package com.example.reclaimbackend.repository;

import com.example.reclaimbackend.model.Claim;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Claims in MongoDB.
 */
@Repository
public interface ClaimRepository extends MongoRepository<Claim, String> {

    /**
     * Claims on one item.
     *
     * @param itemId the item
     * @return claims on that item
     */
    List<Claim> findByItemId(String itemId);

    /**
     * Claims made by one user.
     *
     * @param claimantId the user
     * @return that user's claims
     */
    List<Claim> findByClaimantId(String claimantId);

    /**
     * Claims on one item with one status.
     *
     * @param itemId the item
     * @param status the status
     * @return matching claims
     */
    List<Claim> findByItemIdAndStatus(String itemId, String status);

    /**
     * One person's claims on one item with one status.
     */
    List<Claim> findByItemIdAndClaimantIdAndStatus(
            String itemId, String claimantId, String status);
}
