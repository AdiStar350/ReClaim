package com.example.reclaim.data;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.reclaim.model.Item;

/**
 * A report that could not be posted while the device was offline.
 */
@Entity(tableName = "pending_reports")
public class PendingReportEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String title;
    public String description;
    public String category;
    public String location;
    public String type;
    public String verificationQuestion;
    @Nullable
    public Double latitude;
    @Nullable
    public Double longitude;
    @Nullable
    public String remoteImageUrl;
    @Nullable
    public String localImagePath;
    @Nullable
    public String userId;
    public long createdAtEpochMs;

    public Item toItem() {
        Item item = new Item();
        item.setTitle(title);
        item.setDescription(description);
        item.setCategory(category);
        item.setLocation(location);
        item.setType(type);
        item.setVerificationQuestion(verificationQuestion);
        item.setLatitude(latitude);
        item.setLongitude(longitude);
        item.setImageUrl(remoteImageUrl);
        return item;
    }
}
