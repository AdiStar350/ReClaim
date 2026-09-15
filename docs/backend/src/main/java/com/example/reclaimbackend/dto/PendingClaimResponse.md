```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingClaimResponse {

    private String claimId;
    private String itemId;
    private String itemTitle;
    private String itemImageUrl;
    private String claimantId;
    private String claimantName;
    private String validationAnswer;
    private String status;

    /** The owner's hidden question for this item. */
    private String verificationQuestion;

}
```

What that function does: I show a pending claim with the item title and the owner's question so the owner can review it.
What the function is responsible for: I am the reply shape for the owner's pending-claim list. I mix claim and item fields.
Where it works: I am built by ClaimService.getPendingClaimsForOwner, and ClaimController.getPendingClaimsOnMyItems returns me from GET /api/claims/pending-on-my-items.
Why I chose to write the code of the function the way its written: I put item title, picture, and the hidden question on the claim so the owner does not need extra calls.
More: I use Lombok Builder, which ClaimService uses to fill each field. I have no validation because I am an output, not an input.
