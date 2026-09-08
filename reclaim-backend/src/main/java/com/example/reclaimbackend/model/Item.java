package com.example.reclaimbackend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * A lost or found item in the {@code items} collection.
 * {@code ownerId} is the person who reported it.
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
