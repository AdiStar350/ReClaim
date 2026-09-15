```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CloseReportRequest {

    @NotBlank(message = "Closure reason is required")
    @Pattern(regexp = ClosureReasons.OWNER_INITIATED_PATTERN,
            message = "Closure reason must be 'owner found' or 'owner not found'")
    private String closureReason;

}
```

What that function does: I carry why the owner is closing a report: owner found or owner not found.
What the function is responsible for: I am the body for closing a report. I only allow those two reasons.
Where it works: I am read by ItemController.closeReport on PUT /api/items/{id}/close. The controller then passes closureReason to ItemService.closeReport.
Why I chose to write the code of the function the way its written: I reused ClosureReasons.OWNER_INITIATED_PATTERN so the allowed words live in one place.
More: I import ClosureReasons and use Jakarta NotBlank and Pattern. Lombok fills in getters and constructors.
