```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "claims")
public class Claim {

    @Id
    private String id;

    /** The item being claimed. */
    private String itemId;

    /** Who made the claim. */
    private String claimantId;

    /** Answer to the reporter's hidden question. */
    private String validationAnswer;

    /** "Pending", "Approved", or "Rejected". */
    private String status;

    /**
     * Why it ended. See {@link ClosureReasons}. Empty while still pending.
     */
    private String closureReason;

    /**
     * Whether the claimant has dismissed the decision card.
     */
    private boolean decisionAcknowledged;

    /** Item title. Filled on read, not saved. */
    @Transient
    private String itemTitle;

    /** Item photo URL. Filled on read, not saved. */
    @Transient
    private String itemImageUrl;

    /** "Lost" or "Found". Filled on read, not saved. */
    @Transient
    private String itemType;

}
```

What that function does: I store an ownership claim and a few item fields filled when I am listed for the claimant.
What the function is responsible for: I am the Mongo document in claims. Status starts as PENDING until the owner reviews me.
Where it works: I am saved and updated by ClaimService. ClaimController returns me. ItemService looks up pending claims to set hasUserAlreadyInteracted. Tests build me with Claim.builder().
Why I chose to write the code of the function the way its written: I keep item title, picture, and type as Transient so my-claims can show them without extra client calls.
More: I use Spring Data Document, Id, and Transient. Lombok Builder is how ClaimService.submitClaim creates me.
