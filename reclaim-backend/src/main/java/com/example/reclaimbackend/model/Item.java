package com.example.reclaimbackend.model;

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

    /** Default constructor required by Spring Data. */
    public Item() {
    }

    public Item(String title, String description, String category,
                String location, String status, String type,
                String imageUrl, String ownerId) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.location = location;
        this.status = status;
        this.type = type;
        this.imageUrl = imageUrl;
        this.ownerId = ownerId;
    }

    // ── Getters ──────────────────────────────────────────────────────

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public String getLocation() {
        return location;
    }

    public String getStatus() {
        return status;
    }

    public String getType() {
        return type;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getVerificationQuestion() {
        return verificationQuestion;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Instant getReportedAt() {
        return reportedAt;
    }

    public String getOwnerId() {
        return ownerId;
    }

    // ── Setters ──────────────────────────────────────────────────────

    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setVerificationQuestion(String verificationQuestion) {
        this.verificationQuestion = verificationQuestion;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public void setReportedAt(Instant reportedAt) {
        this.reportedAt = reportedAt;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }
}
