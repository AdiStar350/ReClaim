```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "items")
public class Item {

    @Id
    private String id;

    private String title;

    private String description;

    @Indexed
    private String category;

    private String location;

    /** "OPEN" or "CLOSED". */
    @Indexed
    private String status;

    /**
     * Why it was closed. See {@link ClosureReasons}. Empty while still open.
     */
    private String closureReason;

    /** "Lost" or "Found". */
    @Indexed
    private String type;

    /** Photo URL. */
    private String imageUrl;

    /** Hidden question set by the reporter. */
    private String verificationQuestion;

    private Double latitude;

    private Double longitude;

    @Indexed
    private Instant reportedAt;

    /** Who reported this item. */
    private String ownerId;

    /**
     * If set, this found report is only for the owner of that lost report.
     * It stays off public search and map lists.
     */
    @Indexed
    private String targetLostReportId;

    /** Reporter's name. Filled on read, not saved. */
    @Transient
    private String ownerName;

    /** Reporter's picture URL. Filled on read, not saved. */
    @Transient
    private String ownerProfilePictureUrl;

    /**
     * True if this viewer already claimed a found report
     * or filed a found report for this lost item.
     * Filled on read, not saved.
     */
    @Transient
    private Boolean hasUserAlreadyInteracted;

}
```

What that function does: I store a lost or found report, plus a few extra fields filled when I am read.
What the function is responsible for: I am the Mongo document in items. ownerId is the reporter. Transient fields are for the reply only.
Where it works: I am created, searched, and returned by ItemService and ItemController. ClaimService loads me when reviewing claims. Tests build me with setters.
Why I chose to write the code of the function the way its written: I marked search fields as indexed. I used Transient for name, picture, and the interaction flag so they are not saved.
More: I use Spring Data Document, Id, Indexed, and Transient. Instant is reportedAt. Lombok gives getters and setters used all over the services.
