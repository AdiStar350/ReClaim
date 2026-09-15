```java
@Getter
@Setter
@NoArgsConstructor
public class ClaimRequest {

    @NotBlank(message = "Item ID is required")
    private String itemId;

    @NotBlank(message = "Validation answer is required")
    private String validationAnswer;

}
```

What that function does: I carry the item id and the answer to the hidden question for a new claim.
What the function is responsible for: I am the body for submitting a claim. The claimant comes from the token, not from me.
Where it works: I am read by ClaimController.submitClaim on POST /api/claims. I am then passed to ClaimService.submitClaim.
Why I chose to write the code of the function the way its written: I left the user id off the body so a caller cannot claim as someone else.
More: I use Jakarta NotBlank on both fields. I skipped Builder because the controller just binds JSON onto me.
