```java
public Item() {
}
```

What that function does: I make an empty item so Gson or report code can fill the fields.
What the function is responsible for: I am the no-arg constructor.
Where it works: Gson uses me on item API replies. ReportActivity and PendingReportEntity.toItem also `new Item()`.
Why I chose to write the code of the function the way its written: An empty start lets setters fill only the fields I have.
More: I leave fields null or false until they are set.

```java
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
```

What that function does: I build an item with the main list fields.
What the function is responsible for: I let code make an item without JSON.
Where it works: I did not find an app caller of this constructor. Screens use the empty one and setters.
Why I chose to write the code of the function the way its written: I kept the fields a card needs. Extra fields can be set later.
More: I assign id, title, description, category, location, status, type, imageUrl, and ownerId.

```java
public String getId() {
    return id;
}
```

What that function does: I return the item id.
What the function is responsible for: I identify the report for details, claims, and edits.
Where it works: I am read by ItemNavigationHelper, ItemDetailsActivity, MyItemsFragment, ReportActivity, and adapters.
Why I chose to write the code of the function the way its written: I kept a simple getter.
More: I use Gson @SerializedName("id").

```java
public String getTitle() {
    return title;
}
```

What that function does: I return the title.
What the function is responsible for: I show the name of the report on cards and details.
Where it works: I am read by ItemAdapter, PreviewItemAdapter, MyReportAdapter, ItemFilterHelper, ItemNavigationHelper, SearchFragment, and ReportActivity.
Why I chose to write the code of the function the way its written: I kept a simple getter.
More: I use Gson @SerializedName("title").

```java
public String getDescription() {
    return description;
}
```

What that function does: I return the description.
What the function is responsible for: I give search, details, and edit the longer text.
Where it works: I am read by ItemFilterHelper, ItemNavigationHelper, ReportActivity, and PendingReportStore.
Why I chose to write the code of the function the way its written: I kept a simple getter.
More: I use Gson @SerializedName("description").

```java
public String getCategory() {
    return category;
}
```

What that function does: I return the category.
What the function is responsible for: I filter and show the item type group.
Where it works: I am read by ItemFilterHelper, ItemAdapter, MyReportAdapter, ItemNavigationHelper, and ReportActivity.
Why I chose to write the code of the function the way its written: I kept a simple getter.
More: I use Gson @SerializedName("category").

```java
public String getLocation() {
    return location;
}
```

What that function does: I return the place text.
What the function is responsible for: I show where the item was lost or found.
Where it works: I am read by adapters, ItemFilterHelper, SearchFragment, ItemNavigationHelper, and ReportActivity.
Why I chose to write the code of the function the way its written: I kept a simple getter.
More: I use Gson @SerializedName("location").

```java
public String getStatus() {
    return status;
}
```

What that function does: I return OPEN or CLOSED.
What the function is responsible for: I tell lists and details if the report is still active.
Where it works: I am read by ItemAdapter, HomeFragment, MyItemsFragment, and ItemNavigationHelper.
Why I chose to write the code of the function the way its written: I kept a simple getter.
More: I use Gson @SerializedName("status").

```java
public String getClosureReason() {
    return closureReason;
}
```

What that function does: I return why a closed report was closed.
What the function is responsible for: I explain a closed status when the server sent a reason.
Where it works: Gson fills this field. Details can show it after a server refresh.
Why I chose to write the code of the function the way its written: I kept a simple getter.
More: I use Gson @SerializedName("closureReason").

```java
public String getType() {
    return type;
}
```

What that function does: I return Lost or Found.
What the function is responsible for: I tell filters and cards which kind of report this is.
Where it works: I am read by ItemFilterHelper, adapters, HomeFragment, ReportActivity, and ItemNavigationHelper.
Why I chose to write the code of the function the way its written: I kept a simple getter.
More: I use Gson @SerializedName("type").

```java
public String getImageUrl() {
    return imageUrl;
}
```

What that function does: I return the photo URL.
What the function is responsible for: I let Glide load the picture.
Where it works: I am read by adapters, ReportActivity, ItemNavigationHelper, PendingReportStore, and ReportSyncWorker.
Why I chose to write the code of the function the way its written: I kept a simple getter. The URL may be empty.
More: I use Gson @SerializedName("imageUrl").

```java
public String getVerificationQuestion() {
    return verificationQuestion;
}
```

What that function does: I return the check question.
What the function is responsible for: I show the question on details and the claim dialog.
Where it works: I am read by ItemNavigationHelper, ReportActivity, PendingReportStore, and ItemDetailsActivity via extras.
Why I chose to write the code of the function the way its written: I kept a simple getter.
More: I use Gson @SerializedName("verificationQuestion").

```java
public Double getLatitude() {
    return latitude;
}
```

What that function does: I return the map latitude, or null if none.
What the function is responsible for: I place the pin and compute distance.
Where it works: I am read by SearchFragment, ItemMatchHelper, ItemFilterHelper, ReportActivity, and PendingReportStore.
Why I chose to write the code of the function the way its written: I used Double so a missing pin is null, not 0.
More: I use Gson @SerializedName("latitude").

```java
public Double getLongitude() {
    return longitude;
}
```

What that function does: I return the map longitude, or null if none.
What the function is responsible for: I place the pin with latitude.
Where it works: I am read by SearchFragment, ItemMatchHelper, ItemFilterHelper, ReportActivity, and PendingReportStore.
Why I chose to write the code of the function the way its written: I used Double so a missing pin is null.
More: I use Gson @SerializedName("longitude").

```java
public String getReportedAt() {
    return reportedAt;
}
```

What that function does: I return when the item was reported.
What the function is responsible for: I show the report time on details.
Where it works: I am read by ItemNavigationHelper into ItemDetailsActivity extras.
Why I chose to write the code of the function the way its written: I kept the server string as-is.
More: I use Gson @SerializedName("reportedAt").

```java
public String getOwnerId() {
    return ownerId;
}
```

What that function does: I return the reporter's user id.
What the function is responsible for: I tell details if the viewer owns this item.
Where it works: I am read by ItemNavigationHelper and ItemDetailsActivity.
Why I chose to write the code of the function the way its written: I kept a simple getter.
More: I use Gson @SerializedName("ownerId").

```java
public String getOwnerName() {
    return ownerName;
}
```

What that function does: I return the reporter's name.
What the function is responsible for: I show who posted the report.
Where it works: I am read by ItemNavigationHelper for details extras.
Why I chose to write the code of the function the way its written: I kept a simple getter.
More: I use Gson @SerializedName("ownerName").

```java
public String getOwnerProfilePictureUrl() {
    return ownerProfilePictureUrl;
}
```

What that function does: I return the reporter's picture URL.
What the function is responsible for: I show the owner's avatar on details.
Where it works: I am read by ItemNavigationHelper for details extras.
Why I chose to write the code of the function the way its written: I kept a simple getter.
More: I use Gson @SerializedName("ownerProfilePictureUrl").

```java
public void setOwnerName(String ownerName) {
    this.ownerName = ownerName;
}
```

What that function does: I set the reporter's name.
What the function is responsible for: I let Gson write ownerName.
Where it works: Gson may call me while parsing Item JSON.
Why I chose to write the code of the function the way its written: I kept a setter because the field is not final.
More: I assign ownerName.

```java
public void setOwnerProfilePictureUrl(String ownerProfilePictureUrl) {
    this.ownerProfilePictureUrl = ownerProfilePictureUrl;
}
```

What that function does: I set the reporter's picture URL.
What the function is responsible for: I let Gson write the avatar URL.
Where it works: Gson may call me while parsing Item JSON.
Why I chose to write the code of the function the way its written: I kept a setter because the field is not final.
More: I assign ownerProfilePictureUrl.

```java
public void setId(String id) {
    this.id = id;
}
```

What that function does: I set the item id.
What the function is responsible for: I let Gson write the id.
Where it works: Gson may call me while parsing Item JSON.
Why I chose to write the code of the function the way its written: I kept a setter because the field is not final.
More: I assign id.

```java
public void setTitle(String title) {
    this.title = title;
}
```

What that function does: I set the title.
What the function is responsible for: I fill the title when posting or parsing.
Where it works: I am called by ReportActivity and PendingReportEntity.toItem.
Why I chose to write the code of the function the way its written: I kept a simple setter.
More: I assign title.

```java
public void setDescription(String description) {
    this.description = description;
}
```

What that function does: I set the description.
What the function is responsible for: I fill the description when posting or parsing.
Where it works: I am called by ReportActivity and PendingReportEntity.toItem.
Why I chose to write the code of the function the way its written: I kept a simple setter.
More: I assign description.

```java
public void setCategory(String category) {
    this.category = category;
}
```

What that function does: I set the category.
What the function is responsible for: I fill the category when posting or parsing.
Where it works: I am called by ReportActivity and PendingReportEntity.toItem.
Why I chose to write the code of the function the way its written: I kept a simple setter.
More: I assign category.

```java
public void setLocation(String location) {
    this.location = location;
}
```

What that function does: I set the place text.
What the function is responsible for: I fill the location when posting or parsing.
Where it works: I am called by ReportActivity and PendingReportEntity.toItem.
Why I chose to write the code of the function the way its written: I kept a simple setter.
More: I assign location.

```java
public void setStatus(String status) {
    this.status = status;
}
```

What that function does: I set OPEN or CLOSED.
What the function is responsible for: I let Gson write status.
Where it works: Gson may call me while parsing Item JSON.
Why I chose to write the code of the function the way its written: I kept a simple setter.
More: I assign status.

```java
public void setClosureReason(String closureReason) {
    this.closureReason = closureReason;
}
```

What that function does: I set why the report was closed.
What the function is responsible for: I let Gson write the close reason.
Where it works: Gson may call me while parsing Item JSON.
Why I chose to write the code of the function the way its written: I kept a simple setter.
More: I assign closureReason.

```java
public void setType(String type) {
    this.type = type;
}
```

What that function does: I set Lost or Found.
What the function is responsible for: I fill the type when posting or parsing.
Where it works: I am called by ReportActivity and PendingReportEntity.toItem.
Why I chose to write the code of the function the way its written: I kept a simple setter.
More: I assign type.

```java
public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
}
```

What that function does: I set the photo URL.
What the function is responsible for: I attach a Storage URL after upload or from JSON.
Where it works: I am called by ReportActivity, PendingReportEntity.toItem, and ReportSyncWorker.
Why I chose to write the code of the function the way its written: I kept a simple setter.
More: I assign imageUrl.

```java
public void setVerificationQuestion(String verificationQuestion) {
    this.verificationQuestion = verificationQuestion;
}
```

What that function does: I set the check question.
What the function is responsible for: I fill the question when posting or parsing.
Where it works: I am called by ReportActivity and PendingReportEntity.toItem.
Why I chose to write the code of the function the way its written: I kept a simple setter.
More: I assign verificationQuestion.

```java
public void setLatitude(Double latitude) {
    this.latitude = latitude;
}
```

What that function does: I set the map latitude.
What the function is responsible for: I store the pin's north-south value.
Where it works: I am called by ReportActivity and PendingReportEntity.toItem.
Why I chose to write the code of the function the way its written: I used Double so I can store null when there is no pin.
More: I assign latitude.

```java
public void setLongitude(Double longitude) {
    this.longitude = longitude;
}
```

What that function does: I set the map longitude.
What the function is responsible for: I store the pin's east-west value.
Where it works: I am called by ReportActivity and PendingReportEntity.toItem.
Why I chose to write the code of the function the way its written: I used Double so I can store null when there is no pin.
More: I assign longitude.

```java
public void setReportedAt(String reportedAt) {
    this.reportedAt = reportedAt;
}
```

What that function does: I set when the item was reported.
What the function is responsible for: I let Gson write the report time.
Where it works: Gson may call me while parsing Item JSON.
Why I chose to write the code of the function the way its written: I kept a simple setter.
More: I assign reportedAt.

```java
public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
}
```

What that function does: I set the reporter's user id.
What the function is responsible for: I let Gson write ownerId.
Where it works: Gson may call me while parsing Item JSON.
Why I chose to write the code of the function the way its written: I kept a simple setter.
More: I assign ownerId.

```java
public String getTargetLostReportId() {
    return targetLostReportId;
}
```

What that function does: I return the lost report this found report is aimed at.
What the function is responsible for: I mark a private found report that targets one lost item.
Where it works: I am read by PendingReportStore and ReportActivity when building the body.
Why I chose to write the code of the function the way its written: I kept a simple getter. It may be null.
More: I use Gson @SerializedName("targetLostReportId").

```java
public void setTargetLostReportId(String targetLostReportId) {
    this.targetLostReportId = targetLostReportId;
}
```

What that function does: I set the lost report this found report is aimed at.
What the function is responsible for: I attach a target when posting a private find.
Where it works: I am called by ReportActivity and PendingReportEntity.toItem.
Why I chose to write the code of the function the way its written: I kept a simple setter.
More: I assign targetLostReportId.

```java
public boolean hasUserAlreadyInteracted() {
    return hasUserAlreadyInteracted;
}
```

What that function does: I return true if this viewer already claimed or acted on the item.
What the function is responsible for: I hide claim actions the user already used.
Where it works: I am read by ItemNavigationHelper and ItemDetailsActivity.
Why I chose to write the code of the function the way its written: I named it has... so it reads as a yes/no, not a get.
More: I use Gson @SerializedName("hasUserAlreadyInteracted").

```java
public void setHasUserAlreadyInteracted(boolean hasUserAlreadyInteracted) {
    this.hasUserAlreadyInteracted = hasUserAlreadyInteracted;
}
```

What that function does: I set whether this viewer already acted on the item.
What the function is responsible for: I let Gson write the flag.
Where it works: Gson may call me while parsing Item JSON.
Why I chose to write the code of the function the way its written: I kept a simple setter.
More: I assign hasUserAlreadyInteracted.
