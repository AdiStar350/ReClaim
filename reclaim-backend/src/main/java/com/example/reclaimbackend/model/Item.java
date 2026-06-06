package com.example.reclaimbackend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB document representing a lost or found item.
 * <p>
 * Stored in the {@code items} collection. Each item is linked to
 * a user via {@code ownerId} (the reporter).
 * </p>
 */
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

    /** Current status: "OPEN" or "CLOSED". */
    @Indexed
    private String status;

    /** Type of report: "Lost" or "Found". */
    @Indexed
    private String type;

    /** URL of the item photo stored in Firebase Storage. */
    private String imageUrl;

    /** Hidden verification question set by the reporter. */
    private String verificationQuestion;

    private Double latitude;

    private Double longitude;

    @Indexed
    private Instant reportedAt;

    /** ID of the user who reported this item. */
    private String ownerId;

}
