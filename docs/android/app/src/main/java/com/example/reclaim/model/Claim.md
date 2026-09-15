```java
public Claim() {
}
```

What that function does: I make an empty claim so Gson can fill the fields.
What the function is responsible for: I am the no-arg constructor Gson needs.
Where it works: Gson uses me when getMyClaims, submitClaim, reviewClaim, or acknowledgeClaimDecision parse JSON.
Why I chose to write the code of the function the way its written: Gson needs an empty constructor before setters run.
More: I leave fields null or false until Gson fills them.

```java
public Claim(String id, String itemId, String claimantId,
             String validationAnswer, String status) {
    this.id = id;
    this.itemId = itemId;
    this.claimantId = claimantId;
    this.validationAnswer = validationAnswer;
    this.status = status;
}
```

What that function does: I build a claim with the main fields.
What the function is responsible for: I let code make a claim without JSON.
Where it works: I did not find an app caller of this constructor. Gson uses the empty one.
Why I chose to write the code of the function the way its written: I kept the five fields a claim always has. Extra display fields can be set by Gson.
More: I assign id, itemId, claimantId, validationAnswer, and status.

```java
public String getId() {
    return id;
}
```

What that function does: I return the claim id.
What the function is responsible for: I identify the claim for acknowledge and contact.
Where it works: I am read by HomeFragment and ReportHistoryActivity.
Why I chose to write the code of the function the way its written: I kept a simple getter.
More: I use Gson @SerializedName("id").

```java
public String getItemId() {
    return itemId;
}
```

What that function does: I return the item this claim is about.
What the function is responsible for: I link the claim back to a report.
Where it works: I am used on history and home with this object.
Why I chose to write the code of the function the way its written: I kept a simple getter.
More: I use Gson @SerializedName("itemId").

```java
public String getClaimantId() {
    return claimantId;
}
```

What that function does: I return who made the claim.
What the function is responsible for: I identify the claimant.
Where it works: Gson fills this field. Screens mostly show item title instead.
Why I chose to write the code of the function the way its written: I kept a simple getter.
More: I use Gson @SerializedName("claimantId").

```java
public String getValidationAnswer() {
    return validationAnswer;
}
```

What that function does: I return the check-question answer.
What the function is responsible for: I hold what the claimant typed.
Where it works: Gson fills this field. Pending claims use PendingClaimResponse for the same value.
Why I chose to write the code of the function the way its written: I kept a simple getter.
More: I use Gson @SerializedName("validationAnswer").

```java
public String getStatus() {
    return status;
}
```

What that function does: I return Pending, Approved, or Rejected.
What the function is responsible for: I tell home and history how the claim ended.
Where it works: I am read by HomeFragment and HistoryAdapter.
Why I chose to write the code of the function the way its written: I kept a simple getter.
More: I use Gson @SerializedName("status").

```java
public String getClosureReason() {
    return closureReason;
}
```

What that function does: I return why the claim was closed, if the server sent one.
What the function is responsible for: I explain a closed claim.
Where it works: Gson fills this field. History can show it when present.
Why I chose to write the code of the function the way its written: I kept a simple getter.
More: I use Gson @SerializedName("closureReason").

```java
public String getItemTitle() {
    return itemTitle;
}
```

What that function does: I return the item title for this claim.
What the function is responsible for: I show which report the claim is for without a second fetch.
Where it works: I am read by HistoryAdapter.
Why I chose to write the code of the function the way its written: I keep the title on the claim for the history row.
More: I use Gson @SerializedName("itemTitle").

```java
public String getItemImageUrl() {
    return itemImageUrl;
}
```

What that function does: I return the item photo URL.
What the function is responsible for: I show a thumbnail on the history row.
Where it works: I am read by HistoryAdapter.
Why I chose to write the code of the function the way its written: I keep the image URL on the claim for the list.
More: I use Gson @SerializedName("itemImageUrl").

```java
public String getItemType() {
    return itemType;
}
```

What that function does: I return Lost or Found for the claimed item.
What the function is responsible for: I label the history row by report type.
Where it works: I am read by HistoryAdapter.
Why I chose to write the code of the function the way its written: I keep type on the claim so history does not need the item.
More: I use Gson @SerializedName("itemType").

```java
public boolean isDecisionAcknowledged() {
    return decisionAcknowledged;
}
```

What that function does: I return true if the claimant already saw the decision.
What the function is responsible for: I stop home from showing the same decision again.
Where it works: I am read by HomeFragment when it looks for unseen decisions.
Why I chose to write the code of the function the way its written: I used is... because it is a yes/no.
More: I use Gson @SerializedName("decisionAcknowledged").

```java
public void setId(String id) {
    this.id = id;
}
```

What that function does: I set the claim id.
What the function is responsible for: I let Gson write the id.
Where it works: Gson may call me while parsing Claim JSON.
Why I chose to write the code of the function the way its written: I kept a simple setter.
More: I assign id.

```java
public void setItemId(String itemId) {
    this.itemId = itemId;
}
```

What that function does: I set the item id.
What the function is responsible for: I let Gson write itemId.
Where it works: Gson may call me while parsing Claim JSON.
Why I chose to write the code of the function the way its written: I kept a simple setter.
More: I assign itemId.

```java
public void setClaimantId(String claimantId) {
    this.claimantId = claimantId;
}
```

What that function does: I set who made the claim.
What the function is responsible for: I let Gson write claimantId.
Where it works: Gson may call me while parsing Claim JSON.
Why I chose to write the code of the function the way its written: I kept a simple setter.
More: I assign claimantId.

```java
public void setValidationAnswer(String validationAnswer) {
    this.validationAnswer = validationAnswer;
}
```

What that function does: I set the check-question answer.
What the function is responsible for: I let Gson write the answer.
Where it works: Gson may call me while parsing Claim JSON.
Why I chose to write the code of the function the way its written: I kept a simple setter.
More: I assign validationAnswer.

```java
public void setStatus(String status) {
    this.status = status;
}
```

What that function does: I set the claim status.
What the function is responsible for: I let Gson write status.
Where it works: Gson may call me while parsing Claim JSON.
Why I chose to write the code of the function the way its written: I kept a simple setter.
More: I assign status.

```java
public void setClosureReason(String closureReason) {
    this.closureReason = closureReason;
}
```

What that function does: I set why the claim was closed.
What the function is responsible for: I let Gson write the close reason.
Where it works: Gson may call me while parsing Claim JSON.
Why I chose to write the code of the function the way its written: I kept a simple setter.
More: I assign closureReason.
