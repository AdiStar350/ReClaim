package com.example.reclaimbackend.repository;

import com.example.reclaimbackend.model.Claim;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for {@link Claim} documents.
 */
@Repository
public interface ClaimRepository extends MongoRepository<Claim, String> {

    /**
     * Finds all claims for a specific item.
     *
     * @param itemId the ID of the item
     * @return list of claims on the item
     */
    List<Claim> findByItemId(String itemId);

    /**
     * Finds all claims made by a specific user.
     *
     * @param claimantId the ID of the claimant
     * @return list of claims by the user
     */
    List<Claim> findByClaimantId(String claimantId);

    /**
     * Finds all claims for a specific item with a given status.
     * <p>
     * Used to find remaining PENDING claims that must be rejected
     * when one claim is approved.
     * </p>
     *
     * @param itemId the ID of the item
     * @param status the claim status to filter by
     * @return list of matching claims
     */
    List<Claim> findByItemIdAndStatus(String itemId, String status);
}
