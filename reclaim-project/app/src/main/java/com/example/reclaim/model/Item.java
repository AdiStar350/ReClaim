package com.example.reclaim.model;

import com.google.gson.annotations.SerializedName;

/**
 * Data model representing a lost or found item.
 */
public class Item {

    @SerializedName("id")
    private String id;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("category")
    private String category;

    @SerializedName("location")
    private String location;

    @SerializedName("status")
    private String status;

    @SerializedName("type")
    private String type;

    @SerializedName("imageUrl")
    private String imageUrl;

    @SerializedName("verificationQuestion")
    private String verificationQuestion;

    @SerializedName("latitude")
    private Double latitude;

    @SerializedName("longitude")
    private Double longitude;

    @SerializedName("reportedAt")
    private String reportedAt;

    @SerializedName("ownerId")
    private String ownerId;

    public Item() {
    }

    public Item(String id, String title, String description, String category,
                String location, String status, String type, String imageUrl,
                String ownerId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.location = location;
        this.status = status;
        this.type = type;
        this.imageUrl = imageUrl;
        this.ownerId = ownerId;
    }

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

    public String getReportedAt() {
        return reportedAt;
    }

    public String getOwnerId() {
        return ownerId;
    }

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

    public void setReportedAt(String reportedAt) {
        this.reportedAt = reportedAt;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }
}
