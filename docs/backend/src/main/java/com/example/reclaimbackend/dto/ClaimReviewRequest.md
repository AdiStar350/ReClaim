```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClaimReviewRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "APPROVED|REJECTED",
            message = "Status must be either APPROVED or REJECTED")
    private String status;

}
```

What that function does: I carry whether the owner approved or declined a claim.
What the function is responsible for: I am the body for reviewing a claim. Status must be APPROVED or REJECTED.
Where it works: I am read by ClaimController.reviewClaim on PUT /api/claims/{claimId}/review. The controller then passes status to ClaimService.reviewClaim.
Why I chose to write the code of the function the way its written: I used a pattern so other words cannot slip through as a status.
More: I use Jakarta NotBlank and Pattern. Lombok gives me getters, setters, and a builder.
