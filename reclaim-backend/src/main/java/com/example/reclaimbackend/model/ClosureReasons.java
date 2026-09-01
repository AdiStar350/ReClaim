package com.example.reclaimbackend.model;

/**
 * Canonical values for the {@code closureReason} field on reports
 * ({@link Item}) and {@link Claim}s.
 * <p>
 * A report/claim's {@code closureReason} records why it was closed:
 * <ul>
 *   <li>{@link #OWNER_FOUND} — the reporter closed the report because the
 *       owner/item was successfully reunited (owner-initiated).</li>
 *   <li>{@link #OWNER_NOT_FOUND} — the reporter closed the report because no
 *       owner/item could be found (owner-initiated).</li>
 *   <li>{@link #CLAIM_APPROVED} — the report was closed by approving an
 *       ownership claim.</li>
 *   <li>{@link #CLAIM_DECLINED} — an ownership claim was declined.</li>
 * </ul>
 * </p>
 */
public final class ClosureReasons {

    public static final String OWNER_FOUND = "owner found";
    public static final String OWNER_NOT_FOUND = "owner not found";
    public static final String CLAIM_DECLINED = "claim declined";
    public static final String CLAIM_APPROVED = "claim approved";

    /** Regex of the owner-initiated closure reasons, for request validation. */
    public static final String OWNER_INITIATED_PATTERN = "owner found|owner not found";

    private ClosureReasons() {
    }
}
