package com.example.reclaimbackend.model;

import lombok.NoArgsConstructor;
import lombok.AccessLevel;

/**
 * Why a report or {@link Claim} was closed.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClosureReasons {

    public static final String OWNER_FOUND = "owner found";
    public static final String OWNER_NOT_FOUND = "owner not found";
    public static final String CLAIM_DECLINED = "claim declined";
    public static final String CLAIM_APPROVED = "claim approved";

    /** Allowed reasons when the owner closes a report. */
    public static final String OWNER_INITIATED_PATTERN = "owner found|owner not found";
}
